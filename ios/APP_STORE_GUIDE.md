# Photovance AI: Apple App Store Connect & Publishing Guide

This guide contains everything you need to submit and publish **Photovance AI: Pro Photo Studio** (`com.photovance.app`) on the **Apple App Store** and distribute via **TestFlight**.

---

## 1. App Store Connect Metadata

| Field | Recommended Value | Constraints |
| :--- | :--- | :--- |
| **App Name** | `Photovance AI: Pro Photo Studio` | Max 30 characters |
| **Subtitle** | `Face-Preserving AI Outfits` | Max 30 characters (27 used) |
| **Primary Category** | `Photo & Video` | - |
| **Secondary Category** | `Lifestyle` | - |
| **Bundle ID** | `com.photovance.app` | Matches Android Package ID |
| **SKU** | `PHOTOVANCE_AI_01` | Unique internal SKU |
| **Keywords** | `ai photo editor,face studio,outfit changer,remini,photoleap,dress swap,ai portrait,flux ai,headshot` | Max 100 characters |
| **Support URL** | `https://victorian-consent-jumping-complicated.trycloudflare.com` | Or your domain |
| **Marketing URL** | `https://victorian-consent-jumping-complicated.trycloudflare.com` | - |
| **Privacy Policy URL** | `https://victorian-consent-jumping-complicated.trycloudflare.com/privacy` | Mandatory |

---

## 2. App Store Description (Ready to Copy & Paste)

### Promotional Text (170 characters max):
```text
Transform your selfies into red-carpet gala gowns, Monaco yacht couture, and editorial fashion portraits while preserving 100% of your authentic facial identity!
```

### Full Description:
```text
Step into the future of digital fashion with Photovance AI — the premier AI photo studio engineered to transform your outfits and scene aesthetics while preserving 100% of your facial identity.

Unlike ordinary AI avatar generators that alter your face beyond recognition, Photovance AI combines advanced Diffusion Transformer (DiT) architectures with precision neural facial locking. Your eyes, smile, skin texture, and unique beauty remain completely untouched while your clothing and background are reimagined with hyper-realistic 8K photorealism.

🌟 KEY FEATURES:

👑 100% FACIAL IDENTITY PRESERVATION
Our proprietary Face Lock technology detects 68 critical facial landmarks to ensure you always look authentically like yourself in every generation.

👗 INFINITE DIGITAL WARDROBE
Explore our curated Style Inspiration Reel with instant one-tap transformations:
• Monaco Sunset Yacht (Glittering Gold Chainmail)
• Cannes Gala (Haute Couture Emerald Velvet)
• Maldives Island Beach (Sun-Drenched Red Bikini)
• Tokyo Neon Moto (Edgy Black Leather Biker)
• Sydney Opera House (Opulent Crimson Ballgown)
• Sunlit Botanical Gardens (Summer Rose Floral Sundress)

✨ INTERACTIVE BEFORE & AFTER SLIDER
Inspect the transformation with our fluid 60fps split-screen comparison slider. Tap and hold the "Hold to Compare" button to instantly peek at your original photo.

🎨 MAGIC PROMPT STUDIO & STYLE MODIFIERS
Customize any look with natural language or tap our one-touch modifiers for Golden Hour Lighting, 8K Studio Flash, Pure Silk Draping, and Cinematic Depth of Field.

💾 SAVE & SHARE IN ULTRA HD
Export your creations directly to your Apple Photos library in crystal-clear high resolution, or share directly to Instagram, TikTok, and Snapchat.

Download Photovance AI today and transform your photo portfolio with haute couture fashion!
```

---

## 3. Apple Privacy Nutrition Label Questionnaire

In App Store Connect under **App Privacy**:

1. **Do you collect data from this app?**
   - Select **Yes**.
2. **Photos or Videos**:
   - Purpose: **App Functionality** & **Product Personalization**.
   - Linked to User Identity: **No**.
   - Used for Tracking: **No**.
3. **Diagnostics (Crash Data)**:
   - Purpose: **App Functionality**.
   - Linked to User Identity: **No**.
   - Used for Tracking: **No**.

---

## 4. In-App Purchase & Subscription Setup (StoreKit 2)

Recommended subscription tiers in App Store Connect:

| Product Type | Product ID | Pricing | Free Trial |
| :--- | :--- | :--- | :--- |
| **Weekly Pro** | `com.photovance.weekly.pro` | **$4.99 / week** | 3-Day Free Trial |
| **Annual Pro (Best Value)** | `com.photovance.annual.pro` | **$39.99 / year** | 3-Day Free Trial |
| **50 Generation Credits** | `com.photovance.credits.50` | **$4.99** (Consumable) | None |

---

## 5. How to Build the iOS App (.ipa)

### Option A: Cloud Build via GitHub Actions (From Windows)
1. Push this project to your GitHub repository:
   ```powershell
   git add .
   git commit -m "Add native iOS client"
   git push origin main
   ```
2. Navigate to your repository on GitHub and click the **Actions** tab.
3. The **Build Photovance AI iOS App (.ipa)** workflow will automatically run on a free Apple Silicon (`macos-14`) runner.
4. Once completed, download `PhotovanceAI-iOS-Release.zip` containing the ready-to-install `PhotovanceAI-Release.ipa`!

### Option B: Local Build on Mac
1. Open the project in Xcode:
   ```bash
   open ios/PhotovanceAI.xcodeproj
   ```
2. Or run the 1-click terminal export script:
   ```bash
   chmod +x ios/export_ipa.sh
   ./ios/export_ipa.sh
   ```
3. To distribute to TestFlight, in Xcode select `Product > Archive`, click `Distribute App`, and choose `App Store Connect / TestFlight`.
