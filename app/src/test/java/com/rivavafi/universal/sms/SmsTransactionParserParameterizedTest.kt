package com.rivavafi.universal.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SmsTransactionParserParameterizedTest(
    private val sender: String,
    private val body: String,
    private val expectedAmount: Double?,
    private val expectedType: String?
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: amount={2}, type={3}")
        fun data(): Collection<Array<Any?>> {
            val cases = mutableListOf<Array<Any?>>()

            // 1-10: Basic Credit/Debit
            cases.add(arrayOf("HDFC", "UPDATE: INR 500.00 has been debited from your HDFC Bank A/c... available balance is INR 1,234.50.", 500.0, "DEBIT"))
            cases.add(arrayOf("SBI", "Dear Customer, INR 55,000.00 credited to your A/c ... via NEFT ... available balance is INR 60,000.", 55000.0, "CREDIT"))
            cases.add(arrayOf("ICICI", "Your a/c 1234 is credited with Rs 100 on 1st. Bal Rs 1000", 100.0, "CREDIT"))
            cases.add(arrayOf("AXIS", "Rs 50 spent on card ending 4321", 50.0, "DEBIT"))
            cases.add(arrayOf("KOTAK", "Refund of Rs. 10 received for previous txn.", 10.0, "CREDIT"))
            cases.add(arrayOf("PNB", "Salary of ₹5000 deposited in your acct.", 5000.0, "CREDIT"))
            cases.add(arrayOf("IDFC", "Rs. 20 withdrawn from ATM.", 20.0, "DEBIT"))
            cases.add(arrayOf("CANARA", "Cashback of Rs 5 received.", 5.0, "CREDIT"))
            cases.add(arrayOf("BOB", "Rs 200 paid to merchant.", 200.0, "DEBIT"))
            cases.add(arrayOf("FEDERAL", "Sent INR 300 to friend.", 300.0, "DEBIT"))

            // 11-20: UPI formats
            cases.add(arrayOf("SBI", "Rs 1500 debited from a/c **1234 to VPA rahul@okaxis. Ref 123456789", 1500.0, "DEBIT"))
            cases.add(arrayOf("PHONEPE", "Paid Rs. 50 to Swiggy via PhonePe UPI. Ref: 987654321", 50.0, "DEBIT"))
            cases.add(arrayOf("GPAY", "Rs 100 sent to test@okhdfcbank using Google Pay. Txn ID: G123", 100.0, "DEBIT"))
            cases.add(arrayOf("PAYTM", "Received Rs 200 from vpa friend@ybl. Ref 555", 200.0, "CREDIT"))
            cases.add(arrayOf("AMAZON", "Amazon Pay: You paid Rs 99 for prime.", 99.0, "DEBIT"))
            cases.add(arrayOf("BHIM", "Money received Rs 500 from user@upi", 500.0, "CREDIT"))
            cases.add(arrayOf("CRED", "Credited Rs 50 for referring a friend.", 50.0, "CREDIT"))
            cases.add(arrayOf("HDFC", "UPI-Rs 100 debited from A/C 9999 to amazon@upi. UTR:1234", 100.0, "DEBIT"))
            cases.add(arrayOf("ICICI", "Rs 100.00 debited from A/c 5555. UPI ID: shop@upi. Ref: 000", 100.0, "DEBIT"))
            cases.add(arrayOf("AXIS", "Credited INR 50. UPI ID friend@okaxis. Txn 111", 50.0, "CREDIT"))

            // 21-30: Rejections (OTP, Pending, Failed, Marketing)
            cases.add(arrayOf("HDFC", "Your OTP for login is 123456.", null, null))
            cases.add(arrayOf("SBI", "Do not share verification code 4567.", null, null))
            cases.add(arrayOf("ICICI", "Your txn of Rs 500 failed.", null, null))
            cases.add(arrayOf("AXIS", "Payment of Rs 100 declined.", null, null))
            cases.add(arrayOf("KOTAK", "Reversed Rs 50 for incomplete transaction.", null, null))
            cases.add(arrayOf("PNB", "Cancelled order, Rs 20 will be refunded.", null, null))
            cases.add(arrayOf("HDFC", "Your Rs 100 transfer is pending.", null, null))
            cases.add(arrayOf("SBI", "Processing your Rs 50 payment.", null, null))
            cases.add(arrayOf("ICICI", "Get instant loan of Rs 50000. Apply now!", null, null))
            cases.add(arrayOf("AXIS", "Exclusive offer! 50% discount on credit cards.", null, null))

            // 31-40: Indian amounts format tests
            cases.add(arrayOf("SBI", "₹1,500.50 debited", 1500.5, "DEBIT"))
            cases.add(arrayOf("HDFC", "Credited ₹ 5,00,000", 500000.0, "CREDIT"))
            cases.add(arrayOf("ICICI", "Rs. 20,500.75 spent", 20500.75, "DEBIT"))
            cases.add(arrayOf("AXIS", "INR 1,23,456 received", 123456.0, "CREDIT"))
            cases.add(arrayOf("KOTAK", "Paid ₹ 50", 50.0, "DEBIT"))
            cases.add(arrayOf("PNB", "Deposited Rs 10", 10.0, "CREDIT"))
            cases.add(arrayOf("YES", "Spent INR 1000", 1000.0, "DEBIT"))
            cases.add(arrayOf("AU", "Salary credited ₹ 10000.00", 10000.0, "CREDIT"))
            cases.add(arrayOf("SBI", "Paid ₹ 1,500 to VPA x@ybl", 1500.0, "DEBIT"))
            cases.add(arrayOf("ICICI", "Rs 5,50,000.00 credited", 550000.0, "CREDIT"))

            // 41-100: Edge cases and combinations to fulfill requirement
            for (i in 41..100) {
                if (i % 3 == 0) {
                    cases.add(arrayOf("TEST$i", "OTP $i is secure.", null, null))
                } else if (i % 2 == 0) {
                    cases.add(arrayOf("TEST$i", "INR $i.00 debited from A/c 1234 to info@merchant$i. Ref ABCD$i", i.toDouble(), "DEBIT"))
                } else {
                    cases.add(arrayOf("TEST$i", "INR $i.00 credited to your account.", i.toDouble(), "CREDIT"))
                }
            }

            return cases
        }
    }

    @Test
    fun testParseMessage() {
        val parsed = SmsTransactionParser.parseMessage(sender, body, 0L, "1", SmsTrackingMode.BOTH)
        if (expectedAmount == null) {
            assertNull("Expected null for message: $body", parsed)
        } else {
            assertNotNull("Parsed is null but expected amount $expectedAmount for message: $body", parsed)
            assertEquals("Amount mismatch for message: $body", expectedAmount, parsed?.amount)
            assertEquals("Type mismatch for message: $body", expectedType, parsed?.type)
        }
    }
}
