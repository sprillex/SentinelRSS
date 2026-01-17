project:
  name: "SentinelRSS"
  vision: "Privacy-first, self-hosted RSS recommendation engine with local ML culling."
  target_hardware: "Moto G (2025) - Headless Android Server"
  primary_interface: "Ktor Web Server (Dark Mode HTML/JSON)"

technical_stack:
  language: "Kotlin 2.1+"
  ui_framework: "Jetpack Compose (for local management)"
  web_framework: "Ktor (Server-CIO engine)"
  database: "Room (SQLite) with SQLCipher for encryption"
  ml_engine: "MediaPipe / TensorFlow Lite (Local Inference)"
  background_tasks: "WorkManager (Periodic Fetch & Cull)"

default_configuration:
  settings:
    feed_limit_count: 50
    feed_limit_days: 30
    refresh_interval: 60
    weather_units: "imperial"
    wifi_only: true
    weather_locations:
      - name: "Petersburg"
        lat: 41.8648291
        lon: -83.6859028
      - name: "Ballerat"
        lat: 41.8648291
        lon: -83.6859028

  initial_feeds:
    - { category: "Hackaday", limit: 10, url: "https://hackaday.com/blog/feed/" }
    - { category: "Troy Hunt", limit: 10, url: "https://feeds.feedburner.com/TroyHunt" }
    - { category: "Jeff Geerling", limit: 10, url: "https://www.jeffgeerling.com/blog.xml" }
    - { category: "BHIS", limit: 10, url: "https://www.blackhillsinfosec.com/blog/feed/" }
    - { category: "Local", limit: 5, url: "https://www.toledoblade.com/rss/" }
    - { category: "Local", limit: 5, url: "https://feeds.feedblitz.com/wtol/news&x=1" }
    - { category: "404", limit: 5, url: "https://www.404media.co/rss/" }
    - { category: "Tech", limit: 5, url: "https://techcrunch.com/feed/" }
    - { category: "Tech", limit: 5, url: "https://www.wired.com/feed/rss" }
    - { category: "Science", limit: 5, url: "https://www.newscientist.com/feed/home/?cmpid=RSS%7CNSNS-Home" }
    - { category: "Tech", limit: 5, url: "https://www.engadget.com/rss.xml" }
    - { category: "News", limit: 5, url: "https://feeds.bbci.co.uk/news/world/rss.xml" }

development_phases:
  phase_1_core:
    tasks:
      - "Implement RSS Ingestion Engine using Kotlin Coroutines."
      - "Set up Room database for local encrypted storage."
      - "Initialize Ktor server module for LAN-based content delivery."
  phase_2_intelligence:
    tasks:
      - "Integrate MediaPipe for on-device text embedding."
      - "Develop similarity-scoring logic for interest matching."
      - "Build 'The Cull' mechanism to delete/hide low-scoring articles."
  phase_3_distribution:
    tasks:
      - "Create Dark Mode HTML dashboard (PC-responsive)."
      - "Expose REST API for Pixel 8a mobile viewing."
      - "Optimize battery/background persistence for Moto G."

security_guidelines:
  - "No external cloud API calls for analysis."
  - "All interest data remains on-device only."
  - "Local server accessible only via authenticated LAN subnet."
