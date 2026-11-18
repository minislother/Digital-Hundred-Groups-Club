import '@/utils/request'
const mocks = vi.hoisted(() => ({ message: vi.fn(), confirm: vi.fn(() => new Promise(() => {})), dispatch: vi.fn(), rejected: null }))
vi.mock('axios', () => ({ default: { create: () => ({ interceptors: {
  request: { use: vi.fn() }, response: { use: (ok, rejected) => { mocks.rejected = rejected } }
} }) } }))
vi.mock('element-plus', () => ({ ElMessage: mocks.message, ElMessageBox: { confirm: mocks.confirm } }))
vi.mock('@/store', () => ({ default: { dispatch: mocks.dispatch } }))
vi.mock('@/utils/auth', () => ({ getToken: () => 'token' }))
const mockMessage = mocks.message
const mockConfirm = mocks.confirm
const mockDispatch = mocks.dispatch
const mockResponseRejected = (...args) => mocks.rejected(...args)

describe('Utils:request auth errors', () => {
  beforeEach(() => {
    mockMessage.mockClear()
    mockConfirm.mockClear()
    mockDispatch.mockClear()
  })

  it('handles 401 backend payload with re-login prompt', async() => {
    const error = {
      response: {
        status: 401,
        data: {
          code: 50008,
          message: '未认证或登录已过期'
        }
      }
    }

    await expect(mockResponseRejected(error)).rejects.toBe(error)

    expect(mockMessage).toHaveBeenCalledWith(expect.objectContaining({
      message: '未认证或登录已过期',
      type: 'error'
    }))
    expect(mockConfirm).toHaveBeenCalled()
  })

  it('handles 403 backend payload without re-login prompt', async() => {
    const error = {
      response: {
        status: 403,
        data: {
          message: '没有权限访问该资源'
        }
      }
    }

    await expect(mockResponseRejected(error)).rejects.toBe(error)

    expect(mockMessage).toHaveBeenCalledWith(expect.objectContaining({
      message: '没有权限访问该资源',
      type: 'error'
    }))
    expect(mockConfirm).not.toHaveBeenCalled()
  })
})
