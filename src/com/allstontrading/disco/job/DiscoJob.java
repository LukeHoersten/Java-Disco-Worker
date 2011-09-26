package com.allstontrading.disco.job;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

import com.allstontrading.disco.DiscoMapFunction;
import com.allstontrading.disco.DiscoReduceFunction;

/**
 * @author Luke Hoersten <lhoersten@allstontrading.com>
 * 
 */
public class DiscoJob {

	private static final String RUN_SCRIPT_NAME = "disco_run_generated.sh";
	private static final String DISCO_JOB_FORMAT = "disco job {0} {1} -f .";

	private static final String MAP_FLAG = "--has-map ";
	private static final String REDUCE_FLAG = "--has-reduce ";
	private static final String PREFIX_FLAG = "--prefix=";

	private final String jobName;
	private final String[] inputs;

	private Class<? extends DiscoMapFunction> mapFunctionClass;
	private Class<? extends DiscoReduceFunction> reduceFunctionClass;
	private final String[] args;

	public DiscoJob(final String jobName, final String[] inputs, final String[] args) {
		this.jobName = jobName;
		this.inputs = inputs;
		this.args = args;

		this.mapFunctionClass = null;
		this.reduceFunctionClass = null;
	}

	public void setMapFunction(final Class<? extends DiscoMapFunction> mapFunctionClass) {
		this.mapFunctionClass = mapFunctionClass;
	}

	public void setReduceFunction(final Class<? extends DiscoReduceFunction> reduceFunctionClass) {
		this.reduceFunctionClass = reduceFunctionClass;
	}

	public void submit() throws IOException, InterruptedException {
		final File runScript = new File(RUN_SCRIPT_NAME);
		DiscoWorkerRunScript.generateRunScript(runScript, mapFunctionClass, reduceFunctionClass, args);

		final Process process = runProcess(runScript);
		sendInputsViaStdinTo(process);
		process.waitFor();
	}

	private Process runProcess(final File runScript) throws IOException {
		final String execLine = MessageFormat.format(DISCO_JOB_FORMAT, buildJobString(), runScript.getName());
		return Runtime.getRuntime().exec(execLine);
	}

	private void sendInputsViaStdinTo(final Process process) throws IOException {
		final OutputStream outputStream = process.getOutputStream();
		for (final String input : inputs) {
			outputStream.write(input.getBytes());
			outputStream.write('\n');
		}
		outputStream.close();
	}

	private String buildJobString() {
		final StringBuilder sb = new StringBuilder();
		if (mapFunctionClass != null) {
			sb.append(MAP_FLAG);
		}
		if (reduceFunctionClass != null) {
			sb.append(REDUCE_FLAG);
		}
		if (!jobName.isEmpty()) {
			sb.append(PREFIX_FLAG).append(jobName);
		}
		return sb.toString();
	}

}
