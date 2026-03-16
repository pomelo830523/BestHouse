// 點擊 Extension 圖示時開啟側邊欄（而非 popup）
chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true }).catch(console.error);
