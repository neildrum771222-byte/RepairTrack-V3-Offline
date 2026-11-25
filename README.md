# RepairTrack V3.0 單機版
Android 8.0 以上，Kotlin / Compose / Room。無登入、無雲端 SDK、無 INTERNET 權限。套件 tw.repairtrack.offline，與 V2 分開安裝，不自動搬移舊資料。

## GitHub Actions 編譯（不需要 Android Studio）
1. 解壓 ZIP，建立 GitHub 私人 repository。
2. 上傳 RepairTrack-V3 資料夾**內部**所有檔案至 repository 根目錄，包含隱藏的 `.github/workflows/build.yml`。若網頁拖曳漏掉 .github，使用 Add file > Create new file，輸入 `.github/workflows/build.yml` 並貼上本機同名檔案內容。
3. Actions > Build APK > Run workflow（或 push 自動觸發）。不需要 Secrets 或 google-services.json。
4. 成功後進入該次執行頁面，下載 Artifacts 的 RepairTrack-V3-debug-apk，解壓並安裝 APK。允許該檔案管理器安裝未知來源應用程式。
5. 若失敗，開啟失敗的步驟並提供完整錯誤訊息。

## 操作
新增建案（例如美術1号院），選擇匯入缺失總表，選原始 .xlsx，核對新增/更新筆數後確認。戶別、工項、廠商搜尋皆可篩選狀態。進入明細，拍照或選取照片，填寫備註，選完全修繕或部分修繕。至少一張照片才能提交。既有 Excel 已修繕標記可直接匯入，沒有歷史照片。統計以目前建案為準。匯出五張工作表。備份 ZIP 包含所有建案及照片；還原會取代所有資料，先另存備份。

## 資料限制與注意事項
- 原表讀取「售服缺失總表(住戶端)」及「參數」，使用 A:N 欄及參數對應。僅支援 .xlsx，公式須有已儲存的計算值。
- 識別碼由售服單號、戶別、樓層、缺失文字及重複序號組成；文字修改或完全相同的列重新排序可能造成新筆或對應不確定。更新保留本機狀態與歷史，不自動刪除原表已移除的列。
- 照片 JPEG 縮圖最長邊 1600 像素，存在 Room 私有資料庫。備份未加密，可能含個資；勿公開分享。還原解壓上限 150MB，超過須另行改版。大量照片尚未壓力測試。
- 卸載 APP 或清除資料會遺失本機紀錄；請定期備份。沒有多人同步或權限管理。
- UI 為藍白色功能版，並非逐像素重現設計稿。

## 驗證
JVM Kotlin Excel parser 使用原始公司 Excel 測試：271 筆、12 已銷單、271 唯一 ID。Android SDK/Gradle 在製作環境不可用，未執行 APK 編譯、Room 實機、相機或備份還原測試。正式使用前請先以測試建案驗證。
