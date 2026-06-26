import request from '@/utils/request'

// 读取并返回业务数据或页面状态。
export function getSeeApplications(groupName) {
    return request({
        url: '/application/recApps',
        method: 'post',
        params: { groupName }
    })
}

// 读取并返回业务数据或页面状态。
export function getAppDetails(id) {
    return request({
        url: '/application/recApp',
        method: 'post',
        params: { id }
    });
}

// 处理当前组件的业务逻辑或用户交互。
export function acceptApplication(applicationId) {
    return request({
        url: '/application/accept',
        method: 'post',
        params: { applicationId }
    })
}

// 处理当前组件的业务逻辑或用户交互。
export function rejectApplication(applicationId) {
    return request({
        url: '/application/reject',
        method: 'post',
        params: { applicationId }
    })
}
