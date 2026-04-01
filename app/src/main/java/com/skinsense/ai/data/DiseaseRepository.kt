package com.skinsense.ai.data

import com.skinsense.ai.ui.compose.Disease

/**
 * Disease Data Repository
 * Provides comprehensive disease information
 */
object DiseaseRepository {
    
    /**
     * Get all diseases in the library
     */
    fun getAllDiseases(): List<Disease> = diseases
    
    /**
     * Get disease by ID
     */
    fun getDiseaseById(id: String): Disease? = diseases.find { it.id == id }
    
    /**
     * Get disease by name
     */
    fun getDiseaseByName(name: String): Disease? = 
        diseases.find { it.name.equals(name, ignoreCase = true) }
    
    /**
     * Search diseases by query
     */
    fun searchDiseases(query: String): List<Disease> =
        diseases.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true) ||
            it.aliases.any { alias -> alias.contains(query, ignoreCase = true) }
        }
    
    /**
     * Get diseases by category
     */
    fun getDiseasesByCategory(category: String): List<Disease> =
        diseases.filter { it.category.equals(category, ignoreCase = true) }
    
    
    
    /**
     * Comprehensive disease database
     */
    private val diseases = listOf(
        Disease(
            id = "acne",
            name = "Acne (Acne Vulgaris)",
            category = "Common Skin Condition",
            aliases = listOf("Pimples", "Zits", "Acne Vulgaris"),
            description = "Acne is a skin condition that occurs when hair follicles become plugged with oil and dead skin cells. Hormonal surges (androgens) are the primary driver.",
            symptoms = listOf(
                "Open comedones (blackheads)",
                "Closed comedones (whiteheads)",
                "Inflammatory lesions (papules and pustules)",
                "Cysts and nodules (severe cases)"
            ),
            causes = listOf(
                "Excess sebum production",
                "Hair follicles clogged by keratin",
                "Overgrowth of C. acnes bacteria",
                "Hormonal surges (androgens)"
            ),
            riskFactors = listOf(
                "Age (teenagers)",
                "Hormonal changes",
                "Family history",
                "Medications"
            ),
            recommendedActions = listOf(
                "Topical: Retinoids (Adapalene) and Benzoyl Peroxide",
                "Systemic: Oral antibiotics for inflammation",
                "Isotretinoin (Accutane) for treatment-resistant, scarring acne"
            )
        ),
        Disease(
            id = "actinic_keratosis",
            name = "Actinic Keratosis (AK)",
            category = "Pre-cancerous",
            aliases = listOf("Solar Keratosis"),
            description = "Rough, scaly, 'sandpaper-like' patches on sun-exposed areas. They are caused by DNA mutations in skin cells (keratinocytes) specifically caused by long-term UV radiation.",
            symptoms = listOf(
                "Rough, scaly, 'sandpaper-like' patches",
                "Located on sun-exposed areas",
                "Often easier to feel than to see"
            ),
            causes = listOf(
                "DNA mutations in skin cells",
                "Long-term UV radiation"
            ),
            riskFactors = listOf(
                "Fair skin",
                "History of intense sun exposure",
                "Weakened immune system"
            ),
            recommendedActions = listOf(
                "Physical: Cryotherapy (liquid nitrogen) or curettage",
                "Field Therapy: Prescription creams like 5-Fluorouracil or Tirbanibulin",
                "Regular skin checks"
            )
        ),
        Disease(
            id = "benign_tumors",
            name = "Benign Tumors",
            category = "Benign Growth",
            aliases = listOf("Lipomas", "Dermatofibromas"),
            description = "Genetic factors or localized skin trauma. Lipomas are slow-growing fatty deposits, while Dermatofibromas are firm fibrous nodules.",
            symptoms = listOf(
                "Lipomas: Soft, doughy, and movable under the skin",
                "Dermatofibromas: Firm, often brown/tan bumps that 'dimple' when pinched"
            ),
            causes = listOf(
                "Genetic factors",
                "Localized skin trauma"
            ),
            riskFactors = listOf(
                "Genetics",
                "Age",
                "Minor injuries"
            ),
            recommendedActions = listOf(
                "Generally monitored",
                "Surgical excision if painful or restricted"
            )
        ),
        Disease(
            id = "candidiasis",
            name = "Candidiasis (Yeast Infection)",
            category = "Fungal Infection",
            aliases = listOf("Yeast Infection", "Thrush"),
            description = "Overgrowth of Candida fungi, usually in warm, moist environments (skin folds).",
            symptoms = listOf(
                "Bright red, 'beefy' rash",
                "Distinct satellite lesions (small red pustules outside main rash)",
                "Common in groin, armpits, and under-breast areas"
            ),
            causes = listOf(
                "Overgrowth of Candida fungi",
                "Warm, moist environments"
            ),
            riskFactors = listOf(
                "Diabetes",
                "Obesity",
                "Recent antibiotic use"
            ),
            recommendedActions = listOf(
                "Topical: Antifungals like Nystatin or Clotrimazole",
                "Lifestyle: Keep skin folds dry with absorbent powders",
                "Use moisture-wicking fabrics"
            )
        ),
        Disease(
            id = "eczema",
            name = "Eczema (Atopic Dermatitis)",
            category = "Inflammatory Skin Condition",
            aliases = listOf("Atopic Dermatitis"),
            description = "A dysfunctional skin barrier (often linked to a Filaggrin protein deficiency) and an overactive immune response to environmental triggers like soap or pollen.",
            symptoms = listOf(
                "Intense itching",
                "Redness",
                "'Weeping' clear fluid",
                "Lichenification (thick, leathery skin) from chronic scratching"
            ),
            causes = listOf(
                "Dysfunctional skin barrier (Filaggrin deficiency)",
                "Overactive immune response to triggers"
            ),
            riskFactors = listOf(
                "Family history of allergies/asthma",
                "Environmental allergens"
            ),
            recommendedActions = listOf(
                "Repair: Thick emollients applied immediately after bathing",
                "Control: Topical corticosteroids",
                "Non-steroidal JAK inhibitors (e.g., Ruxolitinib) for localized flares"
            )
        ),
        Disease(
            id = "psoriasis",
            name = "Psoriasis",
            category = "Autoimmune Skin Condition",
            aliases = listOf("Plaque Psoriasis"),
            description = "An autoimmune disorder where T-cells trigger skin cells to grow every 3–4 days instead of every 30 days.",
            symptoms = listOf(
                "Well-defined, raised red plaques",
                "Covered with silvery-white scales",
                "Affects extensor surfaces (elbows, knees) and scalp"
            ),
            causes = listOf(
                "Autoimmune disorder",
                "T-cells trigger rapid skin cell growth"
            ),
            riskFactors = listOf(
                "Family history",
                "Stress",
                "Infections",
                "Cold weather"
            ),
            recommendedActions = listOf(
                "Mild: Vitamin D analogs (Calcipotriene) and steroids",
                "Moderate/Severe: Biologics (injectables targeting cytokines)",
                "Narrowband UVB light therapy"
            )
        ),
        Disease(
            id = "rosacea",
            name = "Rosacea",
            category = "Chronic Skin Condition",
            aliases = listOf("Adult Acne"),
            description = "A combination of neurovascular dysregulation and an immune reaction to Demodex mites living on the skin.",
            symptoms = listOf(
                "Facial flushing",
                "Visible 'spider veins' (telangiectasia)",
                "Acne-like bumps",
                "Lacks blackheads found in true acne"
            ),
            causes = listOf(
                "Neurovascular dysregulation",
                "Immune reaction to Demodex mites"
            ),
            riskFactors = listOf(
                "Fair skin",
                "Sun exposure",
                "Temperature extremes",
                "Spicy foods",
                "Alcohol"
            ),
            recommendedActions = listOf(
                "Topical: Ivermectin (to target mites) or Metronidazole",
                "Procedural: Vascular lasers to shrink visible blood vessels and reduce redness"
            )
        ),
        Disease(
            id = "seborrheic_keratoses",
            name = "Seborrheic Keratoses (SK)",
            category = "Benign Growth",
            aliases = listOf("Barnacles of Aging"),
            description = "Benign growth of keratinocytes, primarily related to aging and genetics. These are not caused by the sun and are not contagious.",
            symptoms = listOf(
                "'Stuck-on' appearance",
                "Look like a drop of brown or black candle wax",
                "Waxy, crumbly, or scaly texture"
            ),
            causes = listOf(
                "Aging",
                "Genetics"
            ),
            riskFactors = listOf(
                "Age (>50)",
                "Family history"
            ),
            recommendedActions = listOf(
                "Medically unnecessary to treat",
                "Cryotherapy or shave excision if itchy or irritated by clothing"
            )
        ),
        Disease(
            id = "skin_cancer",
            name = "Skin Cancer",
            category = "Oncology",
            aliases = listOf("Melanoma", "Basal Cell Carcinoma", "Squamous Cell Carcinoma"),
            description = "Cumulative or intense intermittent UV exposure damaging the DNA of skin cells. Includes Basal Cell, Squamous Cell, and Melanoma.",
            symptoms = listOf(
                "BCC/SCC: Non-healing sores, pearly bumps, or scaly red patches that bleed easily",
                "Melanoma: Pigmented lesions that are Asymmetric, have irregular Borders, multiple Colors, large Diameter, and are Evolving (ABCDE rule)"
            ),
            causes = listOf(
                "Cumulative UV exposure",
                "Intense intermittent UV exposure",
                "DNA damage"
            ),
            riskFactors = listOf(
                "Fair skin",
                "History of sunburns",
                "Excessive UV exposure",
                "Family history"
            ),
            recommendedActions = listOf(
                "Surgery: Mohs Micrographic Surgery (for maximum tissue sparing)",
                "Advanced: Immunotherapy for metastatic cases",
                "URGENT: Consult dermatologist immediately"
            )
        ),
        Disease(
            id = "vitiligo",
            name = "Vitiligo",
            category = "Pigmentation Disorder",
            aliases = listOf("Leukoderma"),
            description = "An autoimmune attack where the body’s immune system destroys melanocytes (pigment-producing cells).",
            symptoms = listOf(
                "Stark white, depigmented patches of skin",
                "Sharp borders",
                "Often appears symmetrically on the face, hands, and feet"
            ),
            causes = listOf(
                "Autoimmune attack on melanocytes"
            ),
            riskFactors = listOf(
                "Family history",
                "Other autoimmune diseases"
            ),
            recommendedActions = listOf(
                "Repigmentation: Topical JAK inhibitors",
                "Excimer laser therapy",
                "Protection: High-SPF sunscreen (depigmented skin has zero natural protection)"
            )
        ),
        Disease(
            id = "warts",
            name = "Warts (Verrucae)",
            category = "Viral Infection",
            aliases = listOf("Verrucae"),
            description = "Infection of the top layer of skin by the Human Papillomavirus (HPV).",
            symptoms = listOf(
                "Small, fleshy bumps with 'cauliflower' texture",
                "Often contain tiny black dots (thrombosed capillaries, or 'seeds')"
            ),
            causes = listOf(
                "Human Papillomavirus (HPV) infection"
            ),
            riskFactors = listOf(
                "Skin breaks/cuts",
                "Walking barefoot in public places",
                "Weakened immune system"
            ),
            recommendedActions = listOf(
                "Destructive: Over-the-counter salicylic acid or professional cryotherapy",
                "Immune-based: Injecting the wart with Candida antigen"
            )
        )
    )
}
