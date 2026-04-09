type TraceDetail = Record<string, unknown>;

type TraceEntry = {
  time: string;
  type: string;
  route: string;
  stack: string[];
  detail: TraceDetail;
};

const traceEntries: TraceEntry[] = [];

export const traceRuntime = (type: string, detail?: TraceDetail) => {
  void type;
  void detail;
};

export const dumpRuntimeTrace = (label: string, extra?: TraceDetail) => {
  void label;
  void extra;
};

export const registerRuntimeTraceInterceptors = () => {
  traceEntries.length = 0;
};
