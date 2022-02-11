
package me.krypek.igb.cl2;

public class IGB_CL2_Exception extends RuntimeException {
	private static final long serialVersionUID = -3981383146499528214L;

	static IGB_Compiler_L2 igb_cl2;

	IGB_CL2_Exception() { super("\nFile: " + igb_cl2.fileNames[igb_cl2.file] + "\nLine: " + (igb_cl2.lines[igb_cl2.file][igb_cl2.line] + 1)); }

	IGB_CL2_Exception(String str) {
		super("\nFile: " + igb_cl2.fileNames[igb_cl2.file] + "\nLine: " + (igb_cl2.lines[igb_cl2.file][igb_cl2.line] + 1) + "\n" + str);
	}

	IGB_CL2_Exception(String str, Exception e) { super(str, e); }

	IGB_CL2_Exception(int line, String str) {
		super("\nFile: " + igb_cl2.fileNames[igb_cl2.file] + "\nLine: " + (igb_cl2.lines[igb_cl2.file][line] + 1) + "\n" + str);
	}

	IGB_CL2_Exception(int file, int line, String str) {
		super("\nFile: " + igb_cl2.fileNames[file] + "\nLine: " + (igb_cl2.lines[igb_cl2.file][line] + 1) + "\n" + str);
	}
}