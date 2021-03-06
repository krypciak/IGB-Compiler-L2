package me.krypek.igb.cl2.solvers;

import static me.krypek.igb.cl1.datatypes.Instruction.Add;
import static me.krypek.igb.cl1.datatypes.Instruction.Math_CW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import me.krypek.igb.cl2.IGB_CL2_Exception.Err;
import me.krypek.igb.cl1.datatypes.Instruction;
import me.krypek.igb.cl2.RAM;
import me.krypek.igb.cl2.datatypes.Array;
import me.krypek.igb.cl2.datatypes.Field;
import me.krypek.igb.cl2.datatypes.Variable;
import me.krypek.utils.Utils;

public class VarSolver {

	public static final Set<String> typeSet = Set.of("var");

	private final EqSolver eqsolver;
	private final RAM ram;

	public VarSolver(EqSolver eqsolver, RAM ram) {
		this.eqsolver = eqsolver;
		this.ram = ram;
	}

	public ArrayList<Instruction> cmd(String cmd, boolean noinit) {
		if(cmd.startsWith("if") || cmd.startsWith("for") || cmd.startsWith("while") || cmd.startsWith("else"))
			return null;

		{
			int valToAdd = 0;
			String name = null;
			if(cmd.startsWith("++")) {
				valToAdd = 1;
				name = cmd.substring(2);
			} else if(cmd.endsWith("++")) {
				valToAdd = 1;
				name = cmd.substring(0, cmd.length() - 2);
			} else if(cmd.startsWith("--")) {
				valToAdd = -1;
				name = cmd.substring(2);
			} else if(cmd.endsWith("--")) {
				valToAdd = -1;
				name = cmd.substring(0, cmd.length() - 2);
			}
			if(name != null) {
				int cell = ram.getVariableCell(name);
				return Utils.listOf(Add(cell, false, valToAdd, cell));
			}
		}

		ArrayList<Instruction> list = new ArrayList<>();

		int eqIndex = cmd.indexOf('=');
		boolean hasEqSign = eqIndex != -1;
		if(hasEqSign)
			for (char c : EqSolver.operators) {
				int index = cmd.indexOf(c + "=");
				if(index != -1) {
					String name = cmd.substring(0, index).strip();
					int cell = ram.getVariableCell(name);
					String eq = cmd.substring(index + 2).strip();
					return eqsolver.solve(name + c + "(" + eq + ")", cell);
				}
			}

		int spaceIndex = cmd.indexOf(' ');
		if(spaceIndex != -1) {
			String first = cmd.substring(0, spaceIndex);
			if(typeSet.contains(first)) {
				if(noinit)
					throw Err.normal("Cannot init variables in for addition field.");

				// var init
				String rest = cmd.substring(spaceIndex + 1).strip();
				int equalsIndex = rest.indexOf('=');
				if(equalsIndex != -1) {
					String name = rest.substring(0, equalsIndex).strip();
					String eq = rest.substring(equalsIndex + 1).strip();
					var pair = ram.preNewVar(name);
					list.addAll(eqsolver.solve(eq, pair.getSecond()));
					ram.newVar(pair.getFirst(), new Variable(pair.getSecond()));
					return list;
				}
				if(rest.contains("(") || rest.contains(")"))
					return null;
				ram.newVar(rest);
				return new ArrayList<>();
			}
			for (String str : typeSet)
				if(first.startsWith(str)) {
					int bracketIndex = cmd.indexOf('[');
					if(bracketIndex != -1) {
						if(noinit)
							throw Err.normal("Cannot init arrays in for addition field.");
						initArray(cmd, bracketIndex);
						return new ArrayList<>();
					}
					break;
				}
		}

		if(!hasEqSign)
			return null;
		String name = cmd.substring(0, eqIndex).strip();
		String eq = cmd.substring(eqIndex + 1).strip();
		int bracketIndex = name.indexOf('[');
		if(bracketIndex != -1) {
			String dims = name.substring(bracketIndex).strip();
			name = name.substring(0, bracketIndex).strip();
			Field[] dimsF = Utils.getArrayElementsFromString(dims, str -> eqsolver.stringToField(str, false), new Field[0], '[', ']',
					e -> Err.normal("Array dimensions syntax error.", e));
			Array arr = ram.getArray(name);

			var obj = arr.getArrayCell(eqsolver, dimsF, eqsolver.temp1);
			list.addAll(obj.getValue3());
			if(!obj.getValue1()) {
				list.addAll(eqsolver.solve(eq, obj.getValue2()));
				return list;
			}
			ArrayList<Instruction> list1 = eqsolver.solve(eq, eqsolver.temp3);
			list.addAll(list1);

			list.add(Math_CW(eqsolver.temp3, obj.getValue2()));
			return list;
		}
		Variable var = ram.getVariable(name);
		if(var.action != null)
			return var.action.apply(eq);

		return eqsolver.solve(eq, var.cell);
	}

	private void initArray(String cmd, int bracketIndex) {
		String type = cmd.substring(0, bracketIndex);
		if(!typeSet.contains(type))
			throw Err.normal("Unknown variable type: \"" + type + "\".");

		String dims = cmd.substring(bracketIndex);
		String[] split = dims.split("=");
		if(split.length == 1)
			throw Err.normal("Array needs to be initalized.");
		if(split.length > 2)
			throw Err.normal("Syntax error.");
		String eq = split[1].strip();
		int lastBracketIndex = split[0].lastIndexOf(']');
		if(lastBracketIndex == -1)
			throw Err.normal("Syntax error.");
		String name = split[0].substring(lastBracketIndex + 1).strip();
		if(name.contains("["))
			throw Err.normal("Bracket syntax error.");

		dims = split[0].substring(0, lastBracketIndex + 1);
		int dimsCount = 0;

		for (char c : dims.toCharArray()) {
			if(Character.isWhitespace(c))
				continue;
			int t1 = dimsCount++ % 2;
			if(c == '[' && t1 == 1 || c == ']' && t1 == 0)
				throw Err.normal("Bracket syntax error.");
		}
		if(dimsCount % 2 == 1)
			throw Err.normal("Please enter a valid amount of brackets.");
		dimsCount /= 2;

		for (String str : typeSet) {
			final String str1 = "new " + str;

			if(eq.startsWith(str1)) {
				String dims1 = eq.substring(str1.length());

				int[] size = Arrays.stream(Utils.getArrayElementsFromString(dims1, str2 -> {
					double val = ram.solveFinalEq(str2);
					if(val % 1 != 0)
						throw Err.normal("Array size has to be an integer.");
					if(val < 0)
						throw Err.normal("Array size cannot be negative.");
					return (int) val;
				}, new Integer[0], '[', ']', e -> Err.normal("Array size bracket syntax error.", e))).mapToInt(Integer::intValue).toArray();
				if(size.length != dimsCount)
					throw Err.normal("Expected " + dimsCount + " dimenstions, insted got " + size.length + ".");

				ram.newArray(name, size);
				return;
			}
		}
		throw Err.normal("Invalid array initalizer: \"" + eq + "\".");
	}

}
