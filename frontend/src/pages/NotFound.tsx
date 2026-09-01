import { LinkButton } from "../components/ui";

export function NotFound() {
  return (
    <div className="mx-auto flex min-h-[60vh] max-w-lg flex-col items-center justify-center px-4 text-center">
      <p className="font-mono text-sm tracking-[0.16em] text-green uppercase">404</p>
      <h1 className="mt-4 text-[clamp(1.9rem,5vw,2.75rem)]">This one has already been traded</h1>
      <p className="mt-3 leading-relaxed text-muted">
        The page you were after is not here. It may have been taken down, or the link may be wrong.
      </p>
      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <LinkButton to="/browse">Browse the board</LinkButton>
        <LinkButton to="/" tone="outline">
          Go home
        </LinkButton>
      </div>
    </div>
  );
}
