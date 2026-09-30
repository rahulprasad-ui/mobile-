package test

import org.jsoup.Jsoup
import org.junit.Test

class TestJsoup {
    @Test
    fun testScrape() {
        val doc = Jsoup.connect("https://www.google.com/finance/quote/RTX:NYSE").get()
        val price = doc.select("div.YMlKec.fxKbKc").first()?.text()

        // Find "Previous close"
        val prevCloseDiv = doc.select("div.P6K39c").find { it.text().contains("Previous close") }
        val prevCloseText = prevCloseDiv?.nextElementSibling()?.text() ?: doc.select("div.gyFHrc:contains(Previous close) > div.P6K39c").first()?.text()

        println("Price: $price")
        println("Previous close: $prevCloseText")
    }
}
