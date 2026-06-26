const getters = {
  sidebar: state => state.app.sidebar,
  device: state => state.app.device,
  token: state => state.user.token,
  avatar: state => state.user.avatar,
  name: state => state.user.name,
  currentClubId: state => state.clubid,
  currentClubName: state => state.clubname,
  isClubSelected: state => state.clubid !== 0
}
export default getters
