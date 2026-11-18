import SvgIcon from '@/components/SvgIcon'

// 通过静态 glob 收集仓库图标，替换 webpack require.context。
const icons = import.meta.glob('./svg/*.svg', { eager: true, query: '?raw', import: 'default' })
export function registerSvgIcons(app) {
  const sprite = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
  sprite.style.display = 'none'
  sprite.setAttribute('aria-hidden', 'true')
  sprite.innerHTML = Object.entries(icons).map(([path, content]) => {
    const id = path.split('/').pop().replace('.svg', '')
    return content.replace(/<svg\b/, `<symbol id="icon-${id}"`).replace('</svg>', '</symbol>')
  }).join('')
  document.body.prepend(sprite)
  app.component('svg-icon', SvgIcon)
}
