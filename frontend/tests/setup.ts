import "@testing-library/jest-dom/vitest";
import { mkdirSync } from "node:fs";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";
mkdirSync("evidencias", { recursive: true });
afterEach(() => cleanup());
