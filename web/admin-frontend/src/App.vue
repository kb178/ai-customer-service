<template>
  <router-view />
</template>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'PingFang SC', 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 全局卡片样式优化 */
.el-card {
  border-radius: 12px !important;
  border: none !important;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04) !important;
  transition: box-shadow 0.3s ease !important;
}
.el-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08) !important;
}

/* 表格优化 */
.el-table {
  border-radius: 8px !important;
  overflow: hidden;
}
.el-table th.el-table__cell {
  background: #fafafa !important;
  color: #303133 !important;
  font-weight: 600 !important;
  font-size: 13px !important;
}
.el-table td.el-table__cell {
  font-size: 13px !important;
}
.el-table .el-table__row {
  transition: background 0.2s !important;
}

/* 按钮圆角 */
.el-button {
  border-radius: 8px !important;
}
.el-button--small {
  border-radius: 6px !important;
}

/* 输入框圆角 */
.el-input__wrapper {
  border-radius: 8px !important;
}
.el-select .el-input__wrapper {
  border-radius: 8px !important;
}

/* 弹窗圆角 */
.el-dialog {
  border-radius: 16px !important;
  overflow: hidden;
}
.el-dialog__header {
  padding: 16px 24px !important;
  border-bottom: 1px solid #f0f0f0;
  margin-right: 0 !important;
}
.el-dialog__body {
  padding: 24px !important;
}
.el-dialog__footer {
  padding: 12px 24px 16px !important;
  border-top: 1px solid #f0f0f0;
}

/* 分页居右 */
.el-pagination {
  justify-content: flex-end !important;
}

/* Tag 圆角 */
.el-tag {
  border-radius: 6px !important;
}

/* 滚动条美化 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

/* 全局过渡 */
.el-fade-enter-active,
.el-fade-leave-active {
  transition: opacity 0.2s ease !important;
}

/* popconfirm 样式 */
.el-popconfirm__main {
  font-size: 14px !important;
}
</style>
