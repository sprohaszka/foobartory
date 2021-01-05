package alma

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import alma.*

class FoobartoryTest {
  @AfterEach
  fun teardown() {
    stock.clear()
  }

  @Test
  fun `isEnoughStock with emptyList should False`() {
    // Given
    val stock = emptyList<Ore>()

    // When
    val result = isEnoughStock(stock)

    // Then
    assertFalse(result)
  }


  @Test
  fun `isEnoughStock with only one Ore should False`() {
    // Given
    val stock = listOf(Bar())

    // When
    val result = isEnoughStock(stock)

    // Then
    assertFalse(result)
  }

  @Test
  fun `isEnoughStock with correct Ore should True`() {
    // Given
    val stock = listOf(Foo(), Bar())

    // When
    val result = isEnoughStock(stock)

    // Then
    assertTrue(result)
  }

  @Test
  fun `Robot one action at a time`() {
    // Given
    val robot = Robot.create()
    val action = Action.MineBar
    robot.act(action)

    // When
    assertThrows(Exception::class.java) {
      // Then
      robot.act(Action.MineFoo)
    }

    assertEquals(Action.Move(action), robot.action)
  }

  @Test
  fun `find right ore to mine`() {
    // Given
    val stock = listOf(Foo(), Bar(), Bar(), Foo(), Foo())

    // When
    val oreToMine = whichOreToMine(stock)
    
    // Then
    assertEquals(Action.MineBar, oreToMine)
  }

  @Test
  fun `find right ore to mine with empty stock`() {
    // Given
    val stock = listOf(Bar(), Bar())

    // When
    val oreToMine = whichOreToMine(stock)
    
    // Then
    assertEquals(Action.MineFoo, oreToMine)
  }

  @Test
  fun `check if extractFoobarElements with not enough stock throw exception`() {
    // Given
    stock.add(Foo())

    // When
    assertThrows(java.util.NoSuchElementException::class.java) {
      // Then
      extractFoobarElements()
    }
  }

  @Test
  fun `scheduleAction`() {
    // Given
    val robot = Robot(0)

    // When
    robot.act(Action.SellFoobar)

    // Then
    assertTrue(robot.action is Action.Move)
    assertFalse(robot.action is Action.SellFoobar)
    assertTrue(robot.scheduleAction is Action.SellFoobar)
  }

  @Test
  fun `canSellFoobarTest is false when 1 robot is schedule to sell and there is no FooBar`() {
    // Given
    val robot = Robot(0)
    robot.act(Action.SellFoobar)
    val robots = listOf(robot)
    val assembly = emptyList<FooBar>()

    // When
    val result = canSellFoobar(robots, assembly)

    // Then
    assertFalse(result)
  }
  @Test
  fun `canSellFoobarTest is false when no robot is schedule to sell and there is no FooBar`() {
    // Given
    val robot = Robot(0)
    robot.act(Action.MineFoo)
    val robots = listOf(robot)
    val assembly = emptyList<FooBar>()

    // When
    val result = canSellFoobar(robots, assembly)

    // Then
    assertFalse(result)
  }
  @Test
  fun `canSellFoobarTest is false when 1 robot is schedule to sell and there is one FooBar`() {
    // Given
    val robot = Robot(0)
    robot.act(Action.SellFoobar)
    val robots = listOf(robot)
    val assembly = listOf(FooBar(Foo(), Bar()))

    // When
    val result = canSellFoobar(robots, assembly)

    // Then
    assertFalse(result)
  }
  @Test
  fun `canSellFoobarTest is true when no robot is schedule to sell and there is one FooBar`() {
    // Given
    val robot = Robot(0)
    robot.act(Action.MineFoo)
    val robots = listOf(robot)
    val assembly = listOf(FooBar(Foo(), Bar()))

    // When
    val result = canSellFoobar(robots, assembly)

    // Then
    assertTrue(result)
  }
}