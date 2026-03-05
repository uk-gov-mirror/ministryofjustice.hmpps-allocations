package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius

fun deliusRiskResponse() = """
{
  "crn": "J678910",
  "name": {
    "forename": "Dylan",
    "middleName": "Adam",
    "surname": "Armstrong"
  },
  "activeRegistrations": [
    {
      "description": "ALT Under MAPPA Arrangements",
      "startDate": "2021-08-30",
      "notes": "Some Notes",
      "flag": {
        "description": "MAPPA Risk"
        }
    },
    {
      "description": "Suicide/self-harm",
      "startDate": "2021-08-30",
      "notes": "Some Notes",
      "flag": {
        "description": "RoSH"
        }
    }
  ],
  "inactiveRegistrations": [
    {
      "description": "Child Protection",
      "startDate": "2021-05-20",
      "endDate": "2021-08-30",
      "notes": "Some Notes.",
      "flag": {
        "description": "Child Protection Flag"
        }
    }
  ]
}
""".trimIndent()
