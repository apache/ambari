/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
import { X } from "lucide-react";
import type { PropsWithChildren, ReactNode } from "react";

type ModalProps = PropsWithChildren<{
  title: string;
  subtitle?: string;
  busy?: boolean;
  footer?: ReactNode;
  onClose: () => void;
}>;

export default function Modal({ title, subtitle, busy, footer, onClose, children }: ModalProps) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={() => !busy && onClose()}>
      <section className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="modal-title" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-header">
          <div>
            <h2 id="modal-title">{title}</h2>
            {subtitle && <p>{subtitle}</p>}
          </div>
          <button className="icon-button" type="button" aria-label="Close" title="Close" disabled={busy} onClick={onClose}>
            <X size={18} />
          </button>
        </header>
        <div className="modal-body">{children}</div>
        {footer && <footer className="modal-footer">{footer}</footer>}
      </section>
    </div>
  );
}
