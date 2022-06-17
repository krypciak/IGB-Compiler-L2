package me.krypek.igb.cl2;

import java.util.ArrayList;

import me.krypek.freeargparser.ArgType;
import me.krypek.freeargparser.ParsedData;
import me.krypek.freeargparser.ParserBuilder;
import me.krypek.igb.cl1.IGB_L1;
import me.krypek.igb.cl1.Instruction;
import me.krypek.igb.cl2.IGB_CL2_Exception.Err;
import me.krypek.igb.cl2.solvers.ControlSolver;
import me.krypek.igb.cl2.solvers.EqSolver;
import me.krypek.igb.cl2.solvers.VarSolver;

public class IGB_CL2 {

	public static void main(String[] args) {
		//@f:off
		ParsedData data = new ParserBuilder()
				.add("cp", "codepath", 		true,	false, 	ArgType.String, 		"The path to the main file.")
				.add("op", "outputpath", 	false,	false, 	ArgType.String, 		"Path to a directory, l1 files will be saved here.")
				.add("ro", "readableOutput",false, 	false,	ArgType.None, 			"If selected, will also save readable l1 files.")
				.add("q", "quiet", 			false,	false,	ArgType.None, 			"If selected, won't print output to terminal.")
				.parse(args);
		//@f:on

		final String mainPath = data.getString("cp");
		final String outputPath = data.getStringOrDef("op", null);
		final boolean readableOutput = data.has("ro");
		final boolean quiet = data.has("q");

		/*
		 * for (int i = 0; i < inputs.length; i++) { String readden =
		 * Utils.readFromFile(codePaths[i], "\n"); if(readden == null) throw new
		 * IGB_CL2_Exception("Error reading file: \"" + codePaths[i] + "\"."); inputs[i]
		 * = readden;
		 * 
		 * fileNames[i] = new File(codePaths[i]).getName(); }
		 */

		IGB_CL2 igb_cl2 = new IGB_CL2();
		IGB_L1[] compiled = igb_cl2.compile(mainPath, outputPath, quiet);

		/*
		 * if(outputPath != null) { File outDir = new File(outputPath); outDir.mkdirs();
		 * for (int i = 0; i < compiled.length; i++) { IGB_L1 l1 = compiled[i]; String
		 * fileName = Utils.getFileNameWithoutExtension(fileNames[i]);
		 * Utils.serialize(l1, outputPath + File.separator + fileName + ".igb_l1");
		 * if(readableOutput) Utils.writeIntoFile(outputPath + File.separator + fileName
		 * + ".igb_l1_readable", l1.toString());
		 * 
		 * } }
		 */
	}

	public PrecompilationFile[] precfA;
	public int file;
	public int line;

	private Functions functions;
	private RAM ram;
	private EqSolver eqsolver;
	private VarSolver varsolver;
	private ControlSolver cntrlsolver;

	public Functions getFunctions() { return functions; }

	public RAM getRAM() { return ram; }

	public EqSolver getEqSolver() { return eqsolver; }

	public VarSolver getVarSolver() { return varsolver; }

	// public PrecompilationFile getCurrentPrecompilationFile() { return
	// precfA[file]; }

	public IGB_CL2() {}

	public IGB_L1[] compile(String mainPath, String outputPath, boolean quiet) {
		Precompilation prec = new Precompilation(mainPath, quiet);
		this.functions = prec.functions;
		this.precfA = prec.precfA;

		IGB_L1[] arr = new IGB_L1[precfA.length];
		// System.out.println(functions);
		// System.out.println("\n\n");

		for (file = 0; file < precfA.length; file++) {
			PrecompilationFile precf = precfA[file];

			ArrayList<Instruction> instList = new ArrayList<>();
			instList.addAll(precf.startInstructions);
			ram = precf.ram;
			eqsolver = new EqSolver(ram, functions);
			varsolver = new VarSolver(eqsolver, ram);
			cntrlsolver = new ControlSolver(functions, varsolver, eqsolver, ram, precf.cmd);
			for (line = 0; line < precf.cmd.length; line++) {
				String cmd = precf.cmd[line];
				ArrayList<Instruction> out = cmd(cmd);
				// System.out.println("cmd: " + cmd + " -> " + out);
				if(out == null)
					throw Err.normal("Unknown command: \"" + cmd + "\".");
				// instList.add(Instruction.Pointer(":null"));
				instList.addAll(out);
			}
			cntrlsolver.checkStack();

			// System.out.println(ram);
			if(instList.size() > precf.lenlimit)
				throw Err.noLine("Instruction length limit reached: " + instList.size() + " out of " + precf.lenlimit + ".");

			arr[file] = new IGB_L1(precf.startline, instList.toArray(Instruction[]::new));
		}

		if(!quiet)
			for (file = 0; file < arr.length; file++) {
				Instruction[] code = arr[file].code;
				System.out.println("File: " + precfA[file].fileName + " ->");
				for (Instruction element : code)
					System.out.println(element);
				System.out.println("\n");
			}
		return arr;
	}

	private ArrayList<Instruction> cmd(String cmd) {
		if(cmd.length() > 0 && cmd.charAt(0) == '$' || cmd.startsWith("final"))
			return new ArrayList<>();

		{
			ArrayList<Instruction> var = varsolver.cmd(cmd, false);
			if(var != null)
				return var;
		}
		{
			ArrayList<Instruction> cntrl = cntrlsolver.cmd(cmd, line);
			if(cntrl != null)
				return cntrl;
		}

		return null;
	}

}
