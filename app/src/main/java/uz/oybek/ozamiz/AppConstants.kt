package uz.oybek.ozamiz

/**
 * Centralized constants for the application.
 * Eliminates hard-coded values scattered across the codebase.
 */
object AppConstants {

    /**
     * Water tracking constants
     */
    object Water {
        const val GLASS_VOLUME_ML = 250
        const val DEFAULT_GLASSES_COUNT = 8
    }

    /**
     * Exercise tracking constants
     */
    object Exercise {
        const val DEFAULT_TARGET_CALORIES = 300
    }

    /**
     * Weight logging constants
     */
    object Weight {
        const val LOG_INTERVAL_DAYS = 7
    }

    /**
     * Food analysis constants
     */
    object Food {
        const val DEFAULT_PAGE_SIZE = 8
    }

    /**
     * Animation durations
     */
    object Animation {
        const val DEFAULT_DURATION_MS = 300L
        const val SHORT_DURATION_MS = 150L
    }
}
