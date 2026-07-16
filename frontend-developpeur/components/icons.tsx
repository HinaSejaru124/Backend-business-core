import type { SVGProps } from "react";

/**
 * Icônes maison — trait fin (1.5), bouts carrés (cohérent avec les angles nets).
 * Monochromes : la couleur vient de `currentColor`. Pas de librairie externe.
 */
function base(props: SVGProps<SVGSVGElement>) {
  return {
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.5,
    strokeLinecap: "square" as const,
    strokeLinejoin: "miter" as const,
    "aria-hidden": true,
    ...props,
  };
}

export function IconMenu(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M3 6h18M3 12h18M3 18h18" />
    </svg>
  );
}
export function IconClose(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M5 5l14 14M19 5L5 19" />
    </svg>
  );
}
export function IconCopy(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <rect x="9" y="9" width="11" height="11" />
      <path d="M5 15H4V4h11v1" />
    </svg>
  );
}
export function IconCheck(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M4 12l5 5L20 6" />
    </svg>
  );
}
export function IconArrowRight(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M4 12h15M13 6l6 6-6 6" />
    </svg>
  );
}
export function IconChevronRight(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M9 5l7 7-7 7" />
    </svg>
  );
}
export function IconTerminal(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <rect x="3" y="4" width="18" height="16" />
      <path d="M7 9l3 3-3 3M13 15h4" />
    </svg>
  );
}
export function IconBraces(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M8 4H7a2 2 0 0 0-2 2v3l-2 3 2 3v3a2 2 0 0 0 2 2h1M16 4h1a2 2 0 0 1 2 2v3l2 3-2 3v3a2 2 0 0 1-2 2h-1" />
    </svg>
  );
}
export function IconLayers(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M12 3l9 5-9 5-9-5 9-5zM3 12l9 5 9-5M3 16l9 5 9-5" />
    </svg>
  );
}
export function IconBolt(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M13 2L4 14h7l-1 8 9-12h-7l1-8z" />
    </svg>
  );
}
export function IconKey(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <circle cx="8" cy="8" r="4" />
      <path d="M11 11l9 9M17 17l2-2M14 14l2-2" />
    </svg>
  );
}
export function IconActivity(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M3 12h4l3 8 4-16 3 8h4" />
    </svg>
  );
}
export function IconBook(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M4 4h9a3 3 0 0 1 3 3v13a2 2 0 0 0-2-2H4V4zM20 4h-1a3 3 0 0 0-3 3v13a2 2 0 0 1 2-2h2V4z" />
    </svg>
  );
}
export function IconSearch(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <circle cx="10.5" cy="10.5" r="6.5" />
      <path d="M20 20l-5-5" />
    </svg>
  );
}
export function IconShield(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6l8-3z" />
    </svg>
  );
}
export function IconExternal(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5" />
    </svg>
  );
}
export function IconEye(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}
export function IconEyeOff(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M4 4l16 16M9.5 9.6A3 3 0 0 0 12 15a3 3 0 0 0 2.4-1.2M6.3 6.4C3.9 7.8 2 12 2 12s3.5 7 10 7c1.7 0 3.2-.4 4.5-1M9.5 5.2A9.9 9.9 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-2.2 3" />
    </svg>
  );
}
export function IconBuilding(p: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.75} {...p}>
      <path d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-6h6v6M9 10h.01M15 10h.01M9 14h.01M15 14h.01" />
    </svg>
  );
}

export function IconLogout(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M10 4H5a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h5M16 8l4 4-4 4M9 12h11" />
    </svg>
  );
}
export function IconCrown(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M3 8l4 4 5-7 5 7 4-4-2 11H5L3 8zM6 21h12" />
    </svg>
  );
}
export function IconUsers(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <circle cx="9" cy="8" r="3.2" />
      <path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6M16.5 6a3 3 0 0 1 0 6M20 20c0-2.6-1.7-4.8-4-5.6" />
    </svg>
  );
}
export function IconWallet(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <path d="M3 7a2 2 0 0 1 2-2h13a1 1 0 0 1 1 1v2M3 7v11a2 2 0 0 0 2 2h14a1 1 0 0 0 1-1v-3M3 7h16" />
      <path d="M21 10v4h-4a2 2 0 0 1 0-4h4z" />
    </svg>
  );
}
export function IconBan(p: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base(p)}>
      <circle cx="12" cy="12" r="9" />
      <path d="M5.6 5.6l12.8 12.8" />
    </svg>
  );
}
