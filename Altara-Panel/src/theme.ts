import { createTheme, type MantineColorsTuple } from '@mantine/core';

/** Altara aqua — matches the in-game Theme.PRIMARY. */
const brand: MantineColorsTuple = [
  '#e0fcff', '#cbf2ff', '#9ae2ff', '#64d2ff', '#3cc4fe',
  '#23bcfe', '#09b8ff', '#00a1e4', '#008fcd', '#007cb6',
];

/** Discord blurple, for anything Discord-linked (and chat reports). */
const discord: MantineColorsTuple = [
  '#eef0ff', '#dcdefa', '#b5baf0', '#8c95e7', '#6975df',
  '#5865f2', '#4752c4', '#3c45a5', '#323a8a', '#282f70',
];

export const theme = createTheme({
  primaryColor: 'brand',
  colors: { brand, discord },
  defaultRadius: 'md',
  fontFamily: 'Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif',
});
