import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
  title: 'jUPnP',
  tagline: 'UPnP/DLNA library for Java',
  favicon: 'img/favicon.ico',

  // Set the production url of your site here
  url: 'https://www.jupnp.org',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'jupnp', // Usually your GitHub org/user name.
  projectName: 'jupnp', // Usually your repo name.

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/jupnp/jupnp/edit/main/website',
        },
        blog: {
          showReadingTime: true,
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/jupnp/jupnp/edit/main/website',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  plugins: [[require.resolve('docusaurus-lunr-search'), {
    disableVersioning: true
  }]],

  themeConfig: {
    // Replace with your project's social card
    image: 'img/logo.png',
    navbar: {
      title: 'jUPnP',
      logo: {
        alt: 'jUPnP Logo',
        src: 'img/logo.png',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'jupnpSidebar',
          position: 'left',
          label: 'Documentation',
        },
        {
          to: 'https://www.javadoc.io/doc/org.jupnp/org.jupnp', 
          label: 'JavaDoc',
          position: 'left'
        },
        {
          href: 'https://github.com/jupnp/jupnp/discussions',
          label: 'Discussions',
          position: 'right',
        },
        {
          href: 'https://github.com/jupnp/jupnp',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      copyright: `© ${new Date().getFullYear()} jUPnP - UPnP/DLNA library for Java`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['bash', 'java']
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
