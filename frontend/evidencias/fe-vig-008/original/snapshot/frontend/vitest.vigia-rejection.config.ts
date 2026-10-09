import {defineConfig} from "vitest/config";
import react from "@vitejs/plugin-react";
export default defineConfig({plugins:[react()],cacheDir:".vigia-marco02-rejection-cache",test:{environment:"jsdom",setupFiles:["tests/setup.ts"],include:["tests/vigia-marco02-rejection.test.tsx"],fileParallelism:false}});
