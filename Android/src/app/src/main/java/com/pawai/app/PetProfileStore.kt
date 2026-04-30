package com.pawai.app

/**
 * Local store for pet profile + feeding history (vector-backed via EmbeddingGemma).
 *
 * Profile lives in DataStore (preferences). Meal embeddings live in a small SQLite
 * table keyed by timestamp. EmbeddingGemma stays resident on NPU/CPU and is used for:
 *   - encoding meal descriptions when logged
 *   - retrieving similar past meals to summarize "eaten less than usual" patterns
 */
data class PetProfile(
    val name: String,
    val species: Species,
    val breed: String,
    val ageYears: Int,
    val weightLbs: Double,
    val dietaryRestrictions: List<String>,
    val dailyCalorieTarget: Int,
)

enum class Species { Dog, Cat }

data class MealEntry(
    val timestamp: Long,
    val foodDescription: String,
    val assessment: String,
    val embedding: FloatArray,
)

interface PetProfileStore {
    suspend fun getProfile(): PetProfile?
    suspend fun saveProfile(profile: PetProfile)

    suspend fun logMeal(foodDescription: String, assessment: String)
    suspend fun recentMeals(sinceMillis: Long): List<MealEntry>

    /**
     * Plain-English summary of recent feeding context to inject into the Gemma prompt,
     * e.g. "Fed 3x today already, yesterday intake was 20% below normal."
     */
    suspend fun feedingHistorySummary(): String
}
