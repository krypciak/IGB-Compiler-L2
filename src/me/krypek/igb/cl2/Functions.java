package me.krypek.igb.cl2;

import static me.krypek.igb.cl1.datatypes.Instruction.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import me.krypek.igb.cl1.IGB_MA;
import me.krypek.igb.cl1.datatypes.Instruction;
import me.krypek.igb.cl2.IGB_CL2_Exception.Err;
import me.krypek.igb.cl2.datatypes.Field;
import me.krypek.igb.cl2.datatypes.function.CompilerFunction;
import me.krypek.igb.cl2.datatypes.function.Function;
import me.krypek.igb.cl2.datatypes.function.FunctionCall;
import me.krypek.igb.cl2.datatypes.function.FunctionCallField;
import me.krypek.igb.cl2.datatypes.function.FunctionCompilerField;
import me.krypek.igb.cl2.datatypes.function.FunctionCompilerField.FunctionCallCompilerField;
import me.krypek.igb.cl2.datatypes.function.FunctionNormalField.FunctionCallNormalField;
import me.krypek.igb.cl2.datatypes.function.FunctionStringField;
import me.krypek.igb.cl2.datatypes.function.FunctionStringField.FunctionCallStringField;
import me.krypek.utils.Pair;
import me.krypek.utils.Utils;

public class Functions {

	public final LinkedHashMap<String, LinkedHashMap<Integer, Function>> functionMap;

	public Functions() {
		functionMap = new LinkedHashMap<>();
		initCompilerFunctions();
	}

	public Function getFunction(String name, int argLength) {
		name = name.strip();

		HashMap<Integer, Function> map = functionMap.get(name);

		if(map == null)
			throw Err.normal("Function: \"" + name + "\" doesn't exist.");

		Function func = map.get(argLength);
		if(func == null)
			throw Err.normal("Function: \"" + name + "\" doesn't exist with " + argLength + " arguments.");
		return func;
	}

	public void addFunction(Function func) {
		LinkedHashMap<Integer, Function> map = functionMap.get(func.name);
		if(map == null) {
			map = new LinkedHashMap<>();
			functionMap.put(func.name, map);
		}

		final int argLength = func.fields.length;
		if(map.containsKey(argLength))
			throw Err.normal("Function: \"" + func.name + "\" with " + argLength + " arguments already exists.");
		map.put(argLength, func);
	}

	private Field getFieldFromCall(FunctionCall call, int index) {
		FunctionCallField f1 = call.fields[index];
		if(f1 instanceof FunctionCallCompilerField)
			return ((FunctionCallCompilerField) f1).field;
		else if(f1 instanceof FunctionCallNormalField)
			return ((FunctionCallNormalField) f1).field;
		throw Err.notPossible();
	}

	private void initCompilerFunctions() {
		addFunction(new CompilerFunction("sqrt", true, Function.fieldsOf(new FunctionCompilerField()), call -> {
			Field f1 = getFieldFromCall(call, 0);
			ArrayList<Instruction> list = new ArrayList<>();
			var pair1 = call.eqsolver.getInstructionsFromField(f1);
			list.addAll(pair1.getSecond());
			list.add(Math_Sqrt(pair1.getFirst(), call.outputCell));
			return list;
		}));

		addFunction(new CompilerFunction("screenUpdate", false, Function.fieldsOf(), call -> Utils.listOf(Device_ScreenUpdate())));
		addFunction(new CompilerFunction("exit", false, Function.fieldsOf(), call -> Utils.listOf(Cell_Jump(-2))));
		addFunction(new CompilerFunction("wait", false, Function.fieldsOf(new FunctionCompilerField()), call -> {
			Field f1 = getFieldFromCall(call, 0);
			if(!f1.isVal())
				throw Err.normal("Wait function argument has to be an integer.");

			double ticks = f1.value;
			if(ticks % 1 != 0)
				throw Err.normal("Wait function argument has to be an integer.");

			return Utils.listOf(Device_Wait((int) ticks));
		}));
		addFunction(new CompilerFunction("dlog", false, Function.fieldsOf(new FunctionCompilerField()), call -> {
			Field f1 = getFieldFromCall(call, 0);

			ArrayList<Instruction> list = new ArrayList<>();
			var numcell1 = call.eqsolver.getNumCell(f1, -1);
			boolean isCell1 = numcell1.getFirst() != null;
			if(isCell1)
				list.addAll(numcell1.getFirst());

			list.add(Device_Log(isCell1, numcell1.getSecond()));
			return list;
		}));

		addFunction(new CompilerFunction("setpixel", false, Function.fieldsOf(new FunctionCompilerField(), new FunctionCompilerField()), call -> {
			Field f1 = getFieldFromCall(call, 0), f2 = getFieldFromCall(call, 1);

			ArrayList<Instruction> list = new ArrayList<>();

			var numcell1 = call.eqsolver.getNumCell(f1, call.eqsolver.temp1);
			boolean isCell1 = numcell1.getFirst() != null;

			var numcell2 = call.eqsolver.getNumCell(f2, call.eqsolver.temp2);
			boolean isCell2 = numcell2.getFirst() != null;

			if(isCell1 && isCell2 && !f1.isVar() && !f2.isVar() && numcell1.getSecond().intValue() == numcell2.getSecond().intValue())
				numcell1 = call.eqsolver.getNumCell(f1, IGB_MA.CHARLIB_TEMP_START + 9);
			if(isCell1)
				list.addAll(numcell1.getFirst());
			if(isCell2)
				list.addAll(numcell2.getFirst());

			list.add(Pixel(isCell1, numcell1.getSecond().intValue(), isCell2, numcell2.getSecond().intValue()));
			return list;
		}));

		addFunction(new CompilerFunction("pixelcache", false, Function.fieldsOf(new FunctionCompilerField()), call -> {
			Field f1 = getFieldFromCall(call, 0);

			ArrayList<Instruction> list = new ArrayList<>();

			var numcell1 = call.eqsolver.getNumCell(f1, -1);
			boolean isCell1 = numcell1.getFirst() != null;
			if(isCell1)
				list.addAll(numcell1.getFirst());

			if(!isCell1)
				return Utils.listOf(Pixel_Cache_Raw(numcell1.getSecond().intValue()));

			list.add(Pixel_Cache(numcell1.getSecond().intValue()));
			return list;
		}));

		addFunction(new CompilerFunction("pixelcache", false,
				Function.fieldsOf(new FunctionCompilerField(), new FunctionCompilerField(), new FunctionCompilerField()), call -> {
					ArrayList<Instruction> list = new ArrayList<>();
					Field f1 = getFieldFromCall(call, 0), f2 = getFieldFromCall(call, 1), f3 = getFieldFromCall(call, 2);

					var numcell1 = call.eqsolver.getNumCell(f1, call.eqsolver.temp1);
					boolean isCell1 = numcell1.getFirst() != null;
					if(isCell1)
						list.addAll(numcell1.getFirst());

					var numcell2 = call.eqsolver.getNumCell(f2, call.eqsolver.temp2);
					boolean isCell2 = numcell2.getFirst() != null;
					if(isCell2)
						list.addAll(numcell2.getFirst());

					var numcell3 = call.eqsolver.getNumCell(f3, call.eqsolver.temp3);
					boolean isCell3 = numcell3.getFirst() != null;
					if(isCell3)
						list.addAll(numcell3.getFirst());

					list.add(Pixel_Cache(isCell1, numcell1.getSecond().intValue(), isCell2, numcell2.getSecond().intValue(), isCell3,
							numcell3.getSecond().intValue()));
					return list;
				}));

		addFunction(new CompilerFunction("getpixel", true, Function.fieldsOf(new FunctionCompilerField(), new FunctionCompilerField()), call -> {
			Field f1 = getFieldFromCall(call, 0), f2 = getFieldFromCall(call, 1);

			ArrayList<Instruction> list = new ArrayList<>();

			var numcell1 = call.eqsolver.getNumCell(f1, call.eqsolver.temp1);
			boolean isCell1 = numcell1.getFirst() != null;

			var numcell2 = call.eqsolver.getNumCell(f2, call.eqsolver.temp2);
			boolean isCell2 = numcell2.getFirst() != null;

			if(isCell1 && isCell2 && !f1.isVar() && !f2.isVar() && numcell1.getSecond().intValue() == numcell2.getSecond().intValue())
				numcell1 = call.eqsolver.getNumCell(f1, IGB_MA.CHARLIB_TEMP_START + 11);

			if(isCell1)
				list.addAll(numcell1.getFirst());
			if(isCell2)
				list.addAll(numcell2.getFirst());

			list.add(Pixel_Get(isCell1, numcell1.getSecond().intValue(), isCell2, numcell2.getSecond().intValue(),
					call.outputCell == -1 ? IGB_MA.FUNC_RETURN : call.outputCell));
			return list;
		}));

		addFunction(new CompilerFunction("getpixel", false, Function.fieldsOf(new FunctionCompilerField(), new FunctionCompilerField(),
				new FunctionCompilerField(), new FunctionCompilerField(), new FunctionCompilerField()), call -> {
					Field f1 = getFieldFromCall(call, 0), f2 = getFieldFromCall(call, 1), f3 = getFieldFromCall(call, 2), f4 = getFieldFromCall(call, 3),
							f5 = getFieldFromCall(call, 4);

					Pair<Integer, ArrayList<Instruction>> pair = call.eqsolver.getInstructionsFromField(f3);
					ArrayList<Instruction> list = pair.getSecond();

					var numcell1 = call.eqsolver.getNumCell(f1, call.eqsolver.temp1);
					boolean isCell1 = numcell1.getFirst() != null;

					var numcell2 = call.eqsolver.getNumCell(f2, call.eqsolver.temp2);
					boolean isCell2 = numcell2.getFirst() != null;

					if(isCell1 && isCell2 && !f1.isVar() && !f2.isVar() && numcell1.getSecond().intValue() == numcell2.getSecond().intValue())
						numcell1 = call.eqsolver.getNumCell(f1, IGB_MA.CHARLIB_TEMP_START + 11);

					if(isCell1)
						list.addAll(numcell1.getFirst());
					if(isCell2)
						list.addAll(numcell2.getFirst());

					int cell1 = switch (f3.fieldType) {
						case Val -> (int) f3.value;
						case Var -> f3.cell;
						default -> throw Err.normal("Getpixel rgb arg 3: R output cell has to be an integer or a variable.");
					};
					int cell2 = switch (f4.fieldType) {
						case Val -> (int) f4.value;
						case Var -> f4.cell;
						default -> throw Err.normal("Getpixel rgb arg 4: R output cell has to be an integer or a variable.");
					};
					int cell3 = switch (f5.fieldType) {
						case Val -> (int) f5.value;
						case Var -> f5.cell;
						default -> throw Err.normal("Getpixel rgb arg 5: B output cell has to be an integer or a variable.");
					};

					int outCell = cell1 == cell2 - 1 && cell1 == cell3 - 2 ? cell1 : IGB_MA.CHARLIB_TEMP_START + 15;

					list.add(Pixel_Get(isCell1, numcell1.getSecond().intValue(), isCell2, numcell2.getSecond().intValue(), outCell));
					if(outCell == IGB_MA.CHARLIB_TEMP_START + 15) {
						list.add(Copy(IGB_MA.CHARLIB_TEMP_START + 15, cell1));
						list.add(Copy(IGB_MA.CHARLIB_TEMP_START + 16, cell2));
						list.add(Copy(IGB_MA.CHARLIB_TEMP_START + 17, cell3));
					}
					return list;
				}));

		addFunction(new CompilerFunction("drawstring", true, Function.fieldsOf(new FunctionCompilerField(), new FunctionCompilerField(),
				new FunctionStringField(), new FunctionStringField(), new FunctionCompilerField()), call -> {

					Field f1 = getFieldFromCall(call, 0), f2 = getFieldFromCall(call, 1), f5 = getFieldFromCall(call, 4);

					String str = ((FunctionCallStringField) call.fields[2]).str;
					char[] charA = str.toCharArray();
					if(charA.length == 0)
						return new ArrayList<>();

					String name = ((FunctionCallStringField) call.fields[3]).str;

					ArrayList<Instruction> list = new ArrayList<>();

					var pair1 = call.eqsolver.getInstructionsFromField(f1, IGB_MA.CHARLIB_X);
					list.addAll(pair1.getSecond());

					var pair2 = call.eqsolver.getInstructionsFromField(f2, IGB_MA.CHARLIB_Y);
					list.addAll(pair2.getSecond());

					var numcell4 = call.eqsolver.getNumCell(f5, IGB_MA.CHARLIB_SPACING);
					boolean isCell4 = numcell4.getFirst() != null;

					if(isCell4)
						list.addAll(numcell4.getFirst());

					list.add(Init(charA[0], IGB_MA.CHARLIB_CHAR));
					list.add(Cell_Call(":f_" + name + "drawchar_3_start"));

					if(charA.length == 1)
						return list;

					list.add(Add(IGB_MA.CHARLIB_X, true, IGB_MA.FUNC_RETURN, IGB_MA.CHARLIB_X));
					list.add(Add(IGB_MA.CHARLIB_X, isCell4, (int) (double) numcell4.getSecond(), IGB_MA.CHARLIB_X));

					for (int i = 1; i < charA.length; i++) {
						char c = charA[i];
						list.add(Init(c, IGB_MA.CHARLIB_CHAR));
						list.add(Cell_Call(":f_" + name + "drawchar_3_start"));
						if(i != charA.length - 1) {
							list.add(Add(IGB_MA.CHARLIB_X, true, IGB_MA.FUNC_RETURN, IGB_MA.CHARLIB_X));
							list.add(Add(IGB_MA.CHARLIB_X, isCell4, (int) (double) numcell4.getSecond(), IGB_MA.CHARLIB_X));
						}
					}

					return list;
				}));

		addFunction(new CompilerFunction("random", true, Function.fieldsOf(new FunctionCompilerField(), new FunctionCompilerField()), call -> {
			Field f1 = getFieldFromCall(call, 0), f2 = getFieldFromCall(call, 1);

			if(!f1.isVal() || !f2.isVal())
				throw Err.normal("random() arguments have to be integers.");

			double min = f1.value;
			double max = f2.value;

			if(min % 1 != 0 || max % 1 != 0)
				throw Err.normal("random() arguments have to be integers.");

			final int outputcell = call.outputCell == -1 ? 0 : call.outputCell;

			return Utils.listOf(Math_Random((int) min, (int) max, outputcell));
		}));

		/*
		 * addFunction(new Function("comment", obj -> { FunctionActionInputString faif =
		 * (FunctionActionInputString) obj; String comment = faif.str;
		 * if(comment.length() < 2 || !(comment.charAt(0) ==
		 * '\"' && comment.charAt(comment.length() - 1) == '\"')) throw new
		 * IGB_CL2_Exception("Comment has to start and end with \"."); return
		 * Utils.listOf(Instruction.Comment(faif.str.substring(1, comment.length() -
		 * 1))); }, -1, false, FunctionActionInput.FunctionActionInputString.class),
		 * false);
		 */
	}

	@Override
	public String toString() {
		if(functionMap.size() == 0)
			return IGB_CL2.getTabs() + "Functions: {}";
		StringBuilder sb = new StringBuilder(IGB_CL2.getTabs() + "Functions: {\n");
		IGB_CL2.toStringTabs++;
		functionMap.forEach((k, v) -> {
			v.forEach((k1, v1) -> {
				if(!(v1 instanceof CompilerFunction)) {
					sb.append(IGB_CL2.getTabs());
					sb.append(v1.toString());
					sb.append("\n");
				}

			});
		});
		IGB_CL2.toStringTabs--;
		sb.append(IGB_CL2.getTabs() + "}\n");
		return sb.toString();
	}
}
