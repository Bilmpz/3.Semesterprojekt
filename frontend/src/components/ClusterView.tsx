
import { useEffect, useState } from 'react'
import { getConversations } from '../api'
import type { Cluster, Conversation } from '../types'

type Props = {
    cluster: Cluster
    onBack: () => void
}

// Viser samtalerne i det cluster, som brugeren har valgt
export function ClusterView({ cluster, onBack }: Props) {

    // Samtaler hentet fra backend
    const [list, setList] = useState<Conversation[]>([])

    // Holder styr på hvilken samtale der er åbnet
    const [open, setOpen] = useState<string | null>(null)

    // Indlæsning og fejl fra API-kaldet
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    // Henter samtaler, når brugeren vælger et cluster
    useEffect(() => {
        let active = true

        // Nulstiller data ved valg af et nyt cluster
        setList([])
        setOpen(null)
        setLoading(true)
        setError(null)

        getConversations(cluster.id)
            .then((conversations) => {
                if (!active) return

                setList(conversations)
                setLoading(false)
            })
            .catch((cause: unknown) => {
                if (!active) return

                setError(
                    cause instanceof Error
                        ? cause.message
                        : 'Kunne ikke hente samtaler'
                )

                setLoading(false)
            })

        // Forhindrer gamle API-svar i at overskrive nye data
        return () => {
            active = false
        }
    }, [cluster.id])

    return (
        <section className="card">

            {/* Tilbage til clusteroversigten */}
            <button onClick={onBack}>
                ← Tilbage
            </button>

            {/* Clusterets navn, beskrivelse og størrelse */}
            <h2>{cluster.title}</h2>
            <p className="muted">
                {cluster.description} · {cluster.count} samtaler
            </p>

            {/* Viser at samtalerne bliver hentet */}
            {loading && (
                <p className="muted">
                    Henter samtaler fra backend...
                </p>
            )}

            {/* Viser fejl hvis API-kaldet mislykkes */}
            {error && (
                <div className="card dashed warning">
                    Kunne ikke hente samtaler: {error}
                </div>
            )}

            {/* Viser besked hvis clusteret er tomt */}
            {!loading && !error && list.length === 0 && (
                <p className="muted">
                    Ingen samtaler fundet i dette cluster.
                </p>
            )}

            {/* Viser samtalerne fra backend */}
            {!loading && !error && list.map((s) => (
                <div key={s.id} className="conversation">

                    {/* Åbner eller lukker samtalens detaljer */}
                    <button
                        className="link"
                        onClick={() =>
                            setOpen(open === s.id ? null : s.id)
                        }
                    >
                        {new Date(s.timestamp).toLocaleString('da-DK')}
                        {' · '}
                        {s.company}
                        {' · '}
                        {s.reason}
                    </button>

                    {/* Viser forklaring og anonymiseret samtaletekst */}
                    {open === s.id && (
                        <div>
                            <p>
                                <strong>Forklaring:</strong> {s.explanation}
                            </p>

                            <pre>{s.transcript}</pre>
                        </div>
                    )}

                </div>
            ))}

        </section>
    )
}
