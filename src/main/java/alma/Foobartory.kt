package alma

import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.Locale
import java.util.UUID
import sun.misc.Signal
import sun.misc.SignalHandler
import kotlin.random.Random

/** Utils */
fun Double.roundToOneDecimal() = String.format(Locale.ENGLISH, "%.1f", this).toDouble()
fun Double.preciseSubtract(value: Double) = BigDecimal.valueOf(this).subtract(BigDecimal.valueOf(value)).toDouble()

/** Data Structure */
sealed class Ore(val id: UUID = UUID.randomUUID(), val name: String)
class Foo(id: UUID = UUID.randomUUID()): Ore(id, "Foo")
class Bar(id: UUID = UUID.randomUUID()): Ore(id, "Bar")

fun barMiningTime(): Double = Random.nextDouble(0.5, 2.0).roundToOneDecimal()

interface MineAction {
  abstract fun createOre(): Ore
}

sealed class Action(val time: Double, val name: String) {
  object Initial: Action(0.0, "Init")
  data class Move(val next: Action): Action(5.0, "Move to a new action --> ${next.name}")
  object MineFoo: Action(1.0, "Mine foo"), MineAction {
    override fun createOre() = Foo()
  }
  object MineBar: Action(barMiningTime(), "Mine Bar"), MineAction {
    override fun createOre() = Bar()
  }
  data class Assemble(val foo: Foo, val bar: Bar): Action(2.0, "Assemble Foobar")
  object SellFoobar: Action(10.0, "Sell foobar")
  data class BuyRobot(val foos: List<Ore>, val money: Int): Action(0.0, "Buy new robot")
}

data class FooBar(val foo: Ore, val bar: Ore)

/** Stock of Foo, Bar and Foobar */
val stock = mutableListOf<Ore>()
fun groupStock(stock: List<Ore>) = stock.groupByTo(
    mutableMapOf(
      Foo::class to mutableListOf<Ore>(),
      Bar::class to mutableListOf<Ore>()
    ),
    { it::class }
  )
fun removeElementFromStock(ore: Ore) = stock.remove(ore)
var assembly = mutableListOf<FooBar>()
var money = 0
fun earnMoney(amount: Int) {
  money += amount
}
fun takeFromMoney(amount: Int) {
  money -= amount
}
val robots = mutableListOf<Robot>(Robot.create(), Robot.create())
var newRobotToAdd = 0

/** MAIN */
fun main() {
  println("START Foobartory!!!")

  while(robots.count() < 6) {
    val clockTime = 100L
    Thread.sleep(clockTime)
    robots.forEach {
      it.work( (clockTime/100.0).roundToOneDecimal() )

      if (it.isAvailable()) {
        val actionToDo = findActionToDo(robots, stock, assembly, money)
        it.act(actionToDo)
      }
    }

    // Do we buy new robot?
    (1..newRobotToAdd).forEach { robots.add(Robot.create()) }
    newRobotToAdd = 0

    val groupedStock = groupStock(stock)
    val nbFoo = groupedStock.get(Foo::class)?.count() ?: 0
    val nbBar = groupedStock.get(Bar::class)?.count() ?: 0
    println("--> Number of Ores: ${stock.count()} | Foo: $nbFoo | Bar: $nbBar")
    println("--> Number of FooBar: ${assembly.count()}")
    println("--> Money earned: $money €")
  }

  val groupedStock = groupStock(stock)
  val nbFoo = groupedStock.get(Foo::class)?.count() ?: 0
  val nbBar = groupedStock.get(Bar::class)?.count() ?: 0
  println("""
  ***************************************************************************************
  ENDED:
  --> Number of Ores: ${stock.count()} | Foo: $nbFoo | Bar: $nbBar
  --> Number of FooBar: ${assembly.count()}
  --> Money earned: $money €
  --> Number of Robots: ${robots.count()}
  ***************************************************************************************
  """)
}

/** Functions */
fun assembleFooBar(foo: Ore, bar: Ore) {
  if ((0..99).random() < 60) {
    println("\tFooBar SUCCESSFULLY assembled: (${foo.id}, ${bar.id})")
    assembly.add(FooBar(foo, bar))
  } else {
    println("\tFooBar FAILED to assembled")
    println("\tFoo(${foo.id}) lost | Bar(${bar.id}) stock again")
    stock.add(bar)
  }
}

fun Action.associatedOre() = when(this) {
      is Action.MineBar -> Bar::class
      is Action.MineFoo -> Foo::class
      else -> null
    }

fun isEnoughStock(stock: List<Ore>, robots: List<Robot>): Boolean =
  groupStock(stock).all { it.value.count() > 0 }

fun extractFoobarElements(): Pair<Foo, Bar> {
  val elements = Pair(
    stock.first { it is Foo } as Foo,
    stock.first { it is Bar } as Bar
  )
  removeElementFromStock(elements.first)
  removeElementFromStock(elements.second)
  return elements
}

fun whichOreToMine(stock: List<Ore>): Action {
  val groupedStock = groupStock(stock)
  val minOre = groupedStock.minByOrNull{ it.value.count() }
  val oreToMine = if(groupedStock.all { it.value == minOre?.value}) null else minOre?.key
  return when (oreToMine) {
    Bar::class -> Action.MineBar
    Foo::class -> Action.MineFoo
    else -> if ((0..1).random() == 0) Action.MineFoo else Action.MineBar
  }
}

fun canSellFoobar(robots: List<Robot>, assembly: List<FooBar>): Boolean {
  return !robots.any { it.scheduleAction is Action.SellFoobar } && assembly.isNotEmpty()
} 

fun canBuyNewRobot(robots: List<Robot>, stock: List<Ore>, money: Int): Boolean {
  return groupStock(stock)[Foo::class]?.let {
    !robots.any { it.scheduleAction is Action.BuyRobot } &&
    (money >= 3) &&
    (it.count() >= 6)
  } ?: false
}

fun findActionToDo(robots: List<Robot>, stock: List<Ore>, assembly: List<FooBar>, money: Int): Action =
  when {
    canBuyNewRobot(robots, stock, money) -> {
      val foos = groupStock(stock)[Foo::class]!!.take(6)
      foos.forEach { removeElementFromStock(it) }
      takeFromMoney(3)
      Action.BuyRobot(foos, 3)
    }
    money >= 3 -> Action.MineFoo //Priority when enough money to buy new robot
    canSellFoobar(robots, assembly) -> Action.SellFoobar
    isEnoughStock(stock, robots) -> {
      val (foo, bar) = extractFoobarElements()
      Action.Assemble(foo, bar)
    }
    else -> whichOreToMine(stock)
  }

/** Robot definition */
class Robot(private val index: Int) {

  var action: Action? = null
    private set
  var lastAction: Action = Action.Initial
    private set
  val scheduleAction: Action?
    get() = action?.let { if (it is Action.Move) it.next else it }
  private var count: Double = 0.0

  companion object {
    private var ROBOT_IDX = 0
    fun create(): Robot = Robot(ROBOT_IDX++)
  }

  init {
    println("New Robot($index) awaking")
  }

  fun act(action: Action) {
    if (this.action == null) {
      val newAction = if (action == this.lastAction) action else Action.Move(action)
      count = newAction.time
      println("Robot($index): Starting ${newAction.name} with duration of ${action.time}")
      this.action = newAction
    } else {
      println("Robot($index): Can't change to action ($action), still ongoin with ${this.action}")
      throw Exception()
    }
  }

  fun work(secondsElapsed: Double) {
    count = count.preciseSubtract(secondsElapsed)
    if (count < 0) count = 0.0

    if (count == 0.0) {
      val endedAction = action
      when (endedAction) {
        is MineAction -> {
          val ore = endedAction.createOre()
          stock.add(ore)
          println("Robot($index): Stock new ${ore.name} (${ore.id})")

          lastAction = endedAction
          action = null
        }
        is Action.Assemble -> {
          assembleFooBar(endedAction.foo, endedAction.bar)

          lastAction = endedAction
          action = null
        }
        is Action.SellFoobar -> {
          val numberToSell = if (assembly.count() > 5) 5 else assembly.count()
          if (numberToSell < 1) throw Exception()
          (1..numberToSell).forEach { assembly.removeAt(0) }
          earnMoney(numberToSell)

          lastAction = endedAction
          action = null          
        }
        is Action.BuyRobot -> {
          newRobotToAdd++

          lastAction = endedAction
          action = null
        }
        is Action.Move -> {
          action = endedAction.next
          count = endedAction.next.time
        }
        is Action.Initial -> {}
      }

      println("Robot($index): Finished ${endedAction?.name}")
    } else {
      println("Robot($index): Doing ${action?.name}. Time remains: $count seconds")
    }
  }

  fun isAvailable() = action == null
}