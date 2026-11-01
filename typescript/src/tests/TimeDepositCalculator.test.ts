import { TimeDeposit } from '../TimeDeposit'
import { TimeDepositCalculator } from '../TimeDepositCalculator'

/*
 * Characterization tests: they pin the behaviour of the original TypeScript updateBalance, which the
 * assignment states is correct. Every expected value was produced by running the unmodified code.
 * Do not edit these tests to make a refactor pass.
 *
 * Monthly interest = balance * annualRate / 12, rounded with Math.round((interest + Number.EPSILON) * 100) / 100,
 * then added to the balance. This rounding differs from the Java solution on some half-cent values (see 1218 below).
 */

const calculator = new TimeDepositCalculator()

const updatedBalance = (planType: string, days: number, balance: number): number => {
  const deposit = new TimeDeposit(1, planType, balance, days)
  calculator.updateBalance([deposit])
  return deposit.balance
}

describe('TimeDepositCalculator.updateBalance (characterization)', () => {
  test.each([
    // basic: 1% a year after the first 30 days -> 1200 * 0.01 / 12 = 1.00
    ['basic', 30, 1200, 1200],
    ['basic', 31, 1200, 1201],
    ['basic', 1000, 1200, 1201],
    // student: 3% a year from day 31 while days < 366 -> 3.00
    ['student', 30, 1200, 1200],
    ['student', 31, 1200, 1203],
    ['student', 365, 1200, 1203],
    ['student', 366, 1200, 1200],
    // premium: 5% a year, only when days > 45 -> 5.00
    ['premium', 30, 1200, 1200],
    ['premium', 45, 1200, 1200],
    ['premium', 46, 1200, 1205],
  ])('%s plan, %i days: %d -> %d', (planType, days, balance, expected) => {
    expect(updatedBalance(planType, days, balance)).toBe(expected)
  })

  test.each(['gold', 'Basic', 'PREMIUM', ''])('plan "%s" earns no interest', (planType) => {
    expect(updatedBalance(planType, 100, 1200)).toBe(1200)
  })

  test.each([
    // 1234567 * 0.01 / 12 = 1028.8058.. -> 1028.81
    ['basic', 1234567, 1235595.81],
    // 1000 * 0.03 / 12 = 2.50 exactly
    ['student', 1000, 1002.5],
    // 1000 * 0.05 / 12 = 4.1666.. -> 4.17
    ['premium', 1000, 1004.17],
    // 10.01 * 0.01 / 12 = 0.00834.. -> 0.01
    ['basic', 10.01, 10.02],
    // 1 * 0.05 / 12 = 0.00416.. -> 0.00
    ['premium', 1, 1],
    ['basic', 0, 0],
  ])('%s plan rounds interest to cents: %d -> %d', (planType, balance, expected) => {
    expect(updatedBalance(planType, 100, balance)).toBe(expected)
  })

  test.each([
    // 1206 * 0.01 / 12 is 1.0050000000000001 as a double -> 1.01
    [1206, 1207.01],
    // 1218 * 0.01 / 12 is 1.015, stored as 1.01499..; times 100 that is 101.4999.., which Math.round alone would
    // round down. Adding Number.EPSILON first lifts it to 101.5000..1, so the TS original gives 1.02.
    // (The Java original gives 1.01 here; each solution keeps its own original behaviour.)
    [1218, 1219.02],
  ])('half-cent interest on basic plan: %d -> %d', (balance, expected) => {
    expect(updatedBalance('basic', 100, balance)).toBe(expected)
  })

  test('updates every deposit in the list independently', () => {
    const deposits = [
      new TimeDeposit(1, 'basic', 1200, 31),
      new TimeDeposit(2, 'student', 1200, 366),
      new TimeDeposit(3, 'premium', 1200, 46),
      new TimeDeposit(4, 'gold', 1200, 100),
    ]

    calculator.updateBalance(deposits)

    expect(deposits.map((d) => d.balance)).toEqual([1201, 1200, 1205, 1200])
  })

  test('changes only the balance', () => {
    const deposit = new TimeDeposit(7, 'premium', 1200, 46)

    calculator.updateBalance([deposit])

    expect(deposit).toEqual({ id: 7, planType: 'premium', balance: 1205, days: 46 })
  })

  test('applies one month of interest per call', () => {
    const deposit = new TimeDeposit(1, 'basic', 1200, 31)

    calculator.updateBalance([deposit])
    calculator.updateBalance([deposit])

    // 1200 -> 1201.00 -> 1201 * 0.01 / 12 = 1.0008.. -> 1.00 -> 1202.00
    expect(deposit.balance).toBe(1202)
  })

  test('an empty list is a no-op', () => {
    const deposits: TimeDeposit[] = []

    calculator.updateBalance(deposits)

    expect(deposits).toEqual([])
  })
})
