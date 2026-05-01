package com.roost.app.runtime

import com.roost.app.data.Category

/**
 * Maps a free-text merchant name to a fixed Category. FastVLM's output is unconstrained, so
 * we don't trust it to pick our 8-bucket taxonomy. Instead we ask it for the merchant name
 * and run that string through a keyword matcher here.
 *
 * The matcher is intentionally a long if/else chain rather than a fancy classifier: it's
 * deterministic, unit-testable, and easy to extend live during the demo if a judge's
 * receipt slips through (which it will).
 */
object CategoryGuesser {

    fun guess(merchantOrText: String): Category {
        val s = merchantOrText.lowercase()
        return when {
            // Food (restaurants, fast food)
            anyOf(s, "chipotle", "mcdonald", "taco", "burger", "pizza", "subway", "panera",
                "starbucks", "dunkin", "deli", "cafe", "bistro", "kitchen", "grill",
                "sushi", "ramen", "pho", "thai", "panda express", "five guys", "in-n-out",
                "wendy", "kfc", "popeyes", "chick-fil-a", "shake shack", "noodle") -> Category.Food

            // Groceries (supermarkets)
            anyOf(s, "trader joe", "whole foods", "safeway", "kroger", "albertsons",
                "ralphs", "vons", "wegmans", "publix", "aldi", "sprouts", "h-e-b",
                "grocer", "supermarket", "market") -> Category.Groceries

            // Gas
            anyOf(s, "shell", "chevron", "exxon", "mobil", "76", "arco", "valero",
                "bp ", "texaco", "speedway", "circle k", "sunoco", "wawa", "gas") -> Category.Gas

            // School
            anyOf(s, "campus", "bookstore", "tuition", "university", "college", "library",
                "textbook", "edu") -> Category.School

            // Entertainment
            anyOf(s, "amc", "regal", "cinemark", "theatre", "theater", "movie", "concert",
                "ticket", "spotify", "netflix", "hulu", "disney+", "youtube", "steam",
                "game", "bowling", "arcade") -> Category.Entertainment

            // Shopping
            anyOf(s, "amazon", "walmart", "best buy", "macy", "nordstrom", "tj maxx",
                "marshalls", "ross", "uniqlo", "h&m", "zara", "nike", "adidas",
                "apple store", "footlocker", "old navy", "gap", "lululemon") -> Category.Shopping

            // Household
            anyOf(s, "target", "ikea", "home depot", "lowe", "bed bath", "container store",
                "costco", "sam's club", "drugstore", "cvs", "walgreens", "rite aid") -> Category.Household

            else -> Category.Other
        }
    }

    private fun anyOf(haystack: String, vararg needles: String): Boolean =
        needles.any { it in haystack }
}
