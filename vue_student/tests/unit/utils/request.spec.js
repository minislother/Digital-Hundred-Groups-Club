let mockResponseRejected

const mockMessage = jest.fn()
const mockConfirm = jest.fn(() => new Promise(() => {}))
const mockDispatch = jest.fn()

jest.mock('axios', () => ({
  create: jest.fn(() => ({
    interceptors: {
      request: {
        use: jest.fn()
      },
      response: {
        use: jest.fn((fulfilled, rejected) => {
          mockResponseRejected = rejected
        })
      }
    }
  }))
}))

jest.mock('element-ui', () => ({
  Message: mockMessage,
  MessageBox: {
    confirm: mockConfirm
  }
}))

jest.mock('@/store', () => ({
  getters: {
    token: ''
  },
  dispatch: mockDispatch
}))

jest.mock('@/utils/auth', () => ({
  getToken: jest.fn(() => 'token')
}))

require('@/utils/request')

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
