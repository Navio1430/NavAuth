import { withBase, defineConfig } from 'vitepress';
import { withSidebar } from 'vitepress-sidebar';

let basePath = '/NavAuth/';

// https://vitepress.dev/reference/site-config
const vitePressOptions = {
  base: basePath,

  head: [['link', { rel: 'icon', href: basePath + 'navauth-logo.svg' }]],
  title: "NavAuth Docs",
  description: "project dcumentation",
  themeConfig: {
    logo: '/navauth-logo.svg',
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Docs', link: '/markdown-examples' }
    ],

    footer: {
      message: `Made by Navio1430 and Contributors with ❤️`,
      copyright: `Copyright © ${new Date().getFullYear()} Sapphire Code sp. z o.o. & Oliwier Fijas
        <br>Powered by Sapphire Code <img src='/sapphire-logo.svg' width='16' alt='Sapphire Code' style='vertical-align: middle; display: inline;'>
        <br><br>Docs generated with VitePress`,
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/Navio1430' },
      { icon: 'discord', link: 'todo' }
    ]
  }
};

const vitePressSidebarOptions = {
  documentRootPath: '/docs',
  hyphenToSpace: true,
  capitalizeFirst: true
};

export default defineConfig(withSidebar(vitePressOptions, vitePressSidebarOptions));