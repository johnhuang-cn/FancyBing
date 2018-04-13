package net.xdevelop.go.preprocess;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;


public class FeatureGenerator {
	private String featurePath;
	private int index = 1000;
	private int lineCount = 0;
	private OutputStream out; 
	private int maxIndex = 2000;
	private Object locker = new Object();
	private long start = System.currentTimeMillis();
	
	private void initNextOutputStream() throws Exception {
		File f = new File(featurePath + index + ".txt");
		if (!f.exists()) f.createNewFile();
		out = new BufferedOutputStream(new FileOutputStream(f, true), 1024 * 64);
	}
	
	public void write2File(List<String> features) {
		features.forEach(s -> {
			try {
				synchronized (locker) {
					out.write(s.getBytes());
					out.write(0x0a);
					lineCount++;
					if (lineCount == 51200) {
						System.out.println("Generated " + featurePath + index + ".txt" + " time: " + ((System.currentTimeMillis() - start) / 1000));
						
						lineCount = 0;
						out.flush();
						out.close();
						index++;
						initNextOutputStream();
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		});
	}
	
	public void write2File(String feature) {
		try {
			synchronized (locker) {
				out.write(feature.getBytes());
				out.write(0x0a);
				lineCount++;
				if (lineCount == 51200) {
					System.out.println("Generated " + featurePath + index + ".txt" + " time: " + ((System.currentTimeMillis() - start) / 1000));
					
					lineCount = 0;
					out.flush();
					out.close();
					index++;
					initNextOutputStream();
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public boolean isStop() {
		return index > maxIndex;
	}

	public void all(int indexBegin, int indexEnd) throws Exception {
		this.index = indexBegin;
		this.maxIndex = indexEnd;
		featurePath = "f:/features/data/train/";
		String tygemRoot = "d:/workspace/data/sgfs/all/";
		String llzeroRoot = "d:/workspace/data/sgfs/zeroplay/";
		
		initNextOutputStream();
		
		String[] tygemFiles = new File(tygemRoot).list();
		String[] llzeroFiles1 = new File(llzeroRoot).list();
		
		System.out.println("Processing...");
		new SpecialFeatureReaderThread(this, "F:/features/data/raw/ladder.txt", 64).start();
		new SpecialFeatureReaderThread(this, "F:/features/data/raw/ko.txt", 32).start();
		
		for (int i = 0; i < 3; i++) {
			new RandomSGFReaderThread(this, tygemRoot, tygemFiles, 0, 350).start();
		}
//		for (int i = 0; i < 3; i++) {
//			new RandomSGFReaderThread(this, llzeroRoot, llzeroFiles1, 0, 350).start();
//		}
	}
	
	public void open(int indexBegin, int indexEnd) throws Exception {
		this.index = indexBegin;
		this.maxIndex = indexEnd;
		featurePath = "f:/features/data/train/train-open/";
		String tygemRoot = "d:/workspace/data/sgfs/all/";
		String llzeroRoot = "d:/workspace/data/sgfs/zeroplay/";
		
		initNextOutputStream();
		
		String[] tygemFiles = new File(tygemRoot).list();
		String[] llzeroFiles1 = new File(llzeroRoot).list();
		
		System.out.println("Processing...");
		new SpecialFeatureReaderThread(this, "F:/features/data/raw/ladder.txt", 64).start();
		new SpecialFeatureReaderThread(this, "F:/features/data/raw/ko.txt", 32).start();
		
		for (int i = 0; i < 3; i++) {
			new RandomSGFReaderThread(this, tygemRoot, tygemFiles, 0, 100).start();
		}
		for (int i = 0; i < 3; i++) {
			new RandomSGFReaderThread(this, llzeroRoot, llzeroFiles1, 0, 100).start();
		}
	}
	
	public void mid(int indexBegin, int indexEnd) throws Exception {
		this.index = indexBegin;
		this.maxIndex = indexEnd;
		featurePath = "f:/features/data/train/train-mid/";
		String tygemRoot = "d:/workspace/data/sgfs/all/";
		String llzeroRoot = "d:/workspace/data/sgfs/zeroplay/";
		
		initNextOutputStream();
		
		String[] tygemFiles = new File(tygemRoot).list();
		String[] llzeroFiles1 = new File(llzeroRoot).list();
		
		System.out.println("Processing...");
		new SpecialFeatureReaderThread(this, "F:/features/data/raw/ladder.txt", 16).start();
		new SpecialFeatureReaderThread(this, "F:/features/data/raw/ko.txt", 8).start();
		new SpecialFeatureReaderThread(this, "F:/features/data/raw/resign.txt", 32).start();
		
		for (int i = 0; i < 3; i++) {
			new RandomSGFReaderThread(this, tygemRoot, tygemFiles, 80, 350).start();
		}
		for (int i = 0; i < 3; i++) {
			new RandomSGFReaderThread(this, llzeroRoot, llzeroFiles1, 80, 350).start();
		}
		for (int i = 0; i < 2; i++) {
			new RandomSGFReaderThread(this, tygemRoot, tygemFiles, 150, 350).start();
		}
	}
	
	
	private void end(int indexBegin, int indexEnd) throws Exception {
		this.index = indexBegin;
		this.maxIndex = indexEnd;
		featurePath = "f:/features/data/train/train-end/";
		String tygemRoot = "d:/workspace/data/sgfs/all/";
		String llzeroRoot = "d:/workspace/data/sgfs/zeroplay/";
		
		initNextOutputStream();
		
		String[] tygemFiles = new File(tygemRoot).list();
		String[] llzeroFiles1 = new File(llzeroRoot).list();
		
		System.out.println("Processing...");
		new SpecialFeatureReaderThread(this, "F:/features/data/raw/ko.txt", 32).start();
		
		for (int i = 0; i < 4; i++) {
			new RandomSGFReaderThread(this, tygemRoot, tygemFiles, 180, 350).start();
		}
		for (int i = 0; i < 2; i++) {
			new RandomSGFReaderThread(this, llzeroRoot, llzeroFiles1, 180, 350).start();
		}
	}
	
	public static void main(String[] args) throws Exception {
		new FeatureGenerator().mid(299, 400);
	}
	
}
