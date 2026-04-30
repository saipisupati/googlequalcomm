# PawAI — Data Layer

Local-only persistence for the PawAI Android app. All data lives in Room/SQLite on
the Galaxy S25 Ultra; nothing leaves the device.

## Schema

| Table | Purpose |
| --- | --- |
| `pets` | Pet profile: species, breed, age, weight, daily calorie target, dietary restrictions. |
| `meals` | One row per scanned/logged meal: food name, brand, portion, FastVLM confidence, Gemma assessment, severity flag, whether the user confirmed the log. FK to `pets` (CASCADE). Indexed on `(petId, timestamp)`. |
| `alerts` | Toxic-food / baseline-shift / missed-meal / overfeeding alerts surfaced to the user, with severity and acknowledgement state. FK to `pets` (CASCADE). |
| `monthly_summaries` | Roll-up of meals older than 90 days: meal count, avg portion, alert count, and a 768-float embedding centroid (3 072 bytes) for that pet/month. Unique on `(petId, year, month)`. |

DB name: `paw_db`. Version 1, `fallbackToDestructiveMigration()` for hackathon
iteration speed. Access via `PawDb.getInstance(context)`.

## Using the Repository from a ViewModel

```kotlin
class PetViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PawRepository(PawDb.getInstance(app))
    val meals = repo.getMealHistoryFlow(petId = 1L)            // Flow<List<Meal>>
    fun logMeal(m: Meal) = viewModelScope.launch { repo.logMeal(m) }
}
```

The ViewModel only ever talks to `PawRepository` — DAOs are implementation
detail. All non-Flow methods are `suspend`.

## Pruning Policy & Storage Budget

`PruneWorker` runs as a weekly `PeriodicWorkRequest` (scheduled in
`PawAIApplication.onCreate`, constraint: battery not low). It calls
`PawRepository.pruneOldData(olderThanDays = 90)` which, inside a single
`db.withTransaction { }`:

1. Reads all meals older than the cutoff.
2. Groups them by `(petId, year, month)` and inserts/replaces a
   `MonthlySummary` row per group (meal count, average portion, alert count,
   embedding centroid placeholder).
3. Deletes the source `meals` rows.

Either the rollup *and* the delete commit together, or neither does — the
originals are never lost without a summary in their place.

**Storage budget — under 100 MB excluding models, even at 5+ years of use:**
- Recent (≤ 90 days) `meals` row: ~250 bytes including index. At 5 meals/day for
  90 days = 450 rows ≈ 110 KB.
- `MonthlySummary` row: ~3.1 KB (dominated by the 3 072-byte embedding).
  60 months × 3.1 KB ≈ 190 KB.
- `alerts` are sparse (a handful per month); negligible.
- Total per pet at 5 years: well under 1 MB. Even with 10 pets it stays
  comfortably under 100 MB; the on-device LiteRT model files are the only
  things that approach that bound, and they're stored separately under
  `/sdcard/Download/`, not in this database.
