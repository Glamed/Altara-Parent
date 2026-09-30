import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';

dayjs.extend(relativeTime);

export const timeAgo = (millis: number) => dayjs(millis).fromNow();
export const dateTime = (millis: number) => dayjs(millis).format('MMM D, YYYY HH:mm');
export const shortId = (id: string) => id.slice(0, 8);
export const titleCase = (value: string) =>
  value.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
