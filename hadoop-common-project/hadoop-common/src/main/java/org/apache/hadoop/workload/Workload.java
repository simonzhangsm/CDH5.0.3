package org.apache.hadoop.workload;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableFactories;
import org.apache.hadoop.io.WritableFactory;
import org.apache.commons.cli.CommandLine;

@InterfaceAudience.Private
public class Workload implements Writable, Comparable<Workload> {

	static { // register a ctor
		WritableFactories.setFactory(Workload.class, new WritableFactory() {
			@Override
			public Writable newInstance() {
				return new Workload();
			}
		});
	}

	private volatile String type = "default";
	private volatile Set<String> args = new TreeSet<String>();

	public Workload() {
	}

	public Workload(Workload wld) {
		this(wld.getType(), wld.getArgs());
	}

	public Workload(String type) {
		this.type = type;
	}

	public Workload(String type, String[] args) {
		this.type = type;
		for (String arg : args)
			this.args.add(arg);
	}

	public Workload(String type, Set<String> args) {
		this.type = type;
		this.args = args;
	}

	public synchronized void setType(String type) {
		this.type = type;
	}

	protected synchronized String getType() {
		return this.type;
	}

	public synchronized void setArgs(Set<String> args) {
		this.args = args;
	}

	public synchronized Set<String> getArgs() {
		return args;
	}

	public synchronized void addArg(String args) {
		if (args == null || args.isEmpty())
			return;

		String Arg = "[" + args + "]";
		this.args.add(Arg);
	}

	public synchronized void addArg(String[] args) {
		if (args == null || args.length == 0)
			return;

		String Arg = "[";
		for (String arg : args)
			Arg += arg + " ";
		Arg = Arg.trim() + "]";
		this.args.add(Arg);
	}

	public synchronized void addArg(CommandLine cmdline) {
		if (cmdline == null)
			return;
		this.addArg(cmdline.getArgs());
	}

	public synchronized void delArg(String arg) {
		if (arg == null || arg.isEmpty())
			return;

		this.args.remove("[" + arg + "]");
	}
	
	public synchronized void delArg(String[] args) {
		if (args == null || args.length == 0)
			return;

		String Arg = "[";
		for (String arg : args)
			Arg += arg + " ";
		Arg = Arg.trim() + "]";
		this.args.remove(Arg);
	}
	
	public synchronized void delArg(CommandLine cmdline) {
		if (cmdline == null)
			return;

		this.args.remove(cmdline.getArgs());
	}

	public synchronized void clearArgs() {
		this.args.clear();
	}

	@Override
	public synchronized void write(DataOutput out) throws IOException {
		Text.writeString(out, type);
		out.writeInt(args.size());

		Iterator<String> iter = args.iterator();
		while (iter.hasNext())
			Text.writeString(out, iter.next());
	}

	@Override
	public synchronized void readFields(DataInput in) throws IOException {
		this.type = Text.readString(in);
		int size = in.readInt();
		for (int i = 0; i < size; i++) {
			this.args.add(Text.readString(in));
		}
	}

	public void embedConf(Configuration conf) {
		if (conf != null && getWorkload(conf) == null)
			conf.set("Workload", this.toString());
	}

	@Override
	public String toString() {
		String toStr = getType() + " ";
		Iterator<String> iter = args.iterator();
		while (iter.hasNext())
			toStr += iter.next();
		return toStr;
	}

	@Override
	public int compareTo(Workload wld) {
		return (type.equals(wld.getType()) && args.equals(wld.getArgs())) ? 1
				: 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Workload)) {
			return false;
		}
		return compareTo((Workload) o) == 0;
	}

	public static String getWorkload(final Configuration conf) {
		if (conf == null)
			return null;
		return conf.get("Workload");
	}
}
