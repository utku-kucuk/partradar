const steps = [
  {
    title: "Describe the part",
    body: "Enter your vehicle (make, model, year or VIN) and the part you need, once.",
  },
  {
    title: "Nearby shops get the request",
    body: "PartRadar routes it to the shops most likely to have it, based on distance and track record.",
  },
  {
    title: "Compare real answers",
    body: "Shops reply with availability, price and delivery time. Pick one and pick it up today.",
  },
];

export default function Home() {
  return (
    <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-16 px-4 py-20 sm:px-8">
      <section className="flex flex-col gap-6">
        <p className="text-sm font-semibold tracking-widest text-emerald-600 uppercase">
          PartRadar
        </p>
        <h1 className="max-w-3xl text-4xl font-bold tracking-tight sm:text-6xl">
          Find the car part you need, in stock at a shop near you.
        </h1>
        <p className="max-w-2xl text-lg text-neutral-600 dark:text-neutral-400">
          Stop calling shops one by one while your car waits in the garage. Ask once, and let nearby
          parts shops answer with availability, price and delivery time.
        </p>
      </section>

      <section className="grid gap-6 sm:grid-cols-3">
        {steps.map((step, index) => (
          <article
            key={step.title}
            className="rounded-2xl border border-neutral-200 p-6 dark:border-neutral-800"
          >
            <span className="font-mono text-sm text-neutral-500">0{index + 1}</span>
            <h2 className="mt-2 text-lg font-semibold">{step.title}</h2>
            <p className="mt-2 text-sm text-neutral-600 dark:text-neutral-400">{step.body}</p>
          </article>
        ))}
      </section>
    </main>
  );
}
