package com.dragonbudget.app.data

/**
 * Fixed set of categories the user can spend in.
 * Stored as a string in the DB so adding/removing a value is just an enum change.
 */
enum class Category(val displayName: String) {
    Food("Food"),
    Groceries("Groceries"),
    Gas("Gas"),
    School("School"),
    Entertainment("Entertainment"),
    Shopping("Shopping"),
    Household("Household"),
    Other("Other"),
    ;

    companion object {
        fun fromName(name: String): Category =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Other
    }
}
