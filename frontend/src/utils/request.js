import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    } else {
      if (!(res.code === 400 && res.fieldErrors)) {
        ElMessage.error(res.message || '请求失败')
      }
      const error = new Error(res.message || '请求失败')
      error.fieldErrors = res.fieldErrors || null
      error.code = res.code
      return Promise.reject(error)
    }
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
