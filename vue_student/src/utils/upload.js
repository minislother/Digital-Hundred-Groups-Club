import shared from '@baituan/frontend-shared'

// 处理文件上传相关逻辑。
export function buildUploadAction(path) {
  return shared.buildUploadAction(process.env.VUE_APP_BASE_API || '', path)
}
