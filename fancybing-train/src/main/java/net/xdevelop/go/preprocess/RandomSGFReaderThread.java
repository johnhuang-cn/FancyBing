package net.xdevelop.go.preprocess;

import java.util.List;
import java.util.Random;

import net.xdevelop.go.preprocess.SgfFeatureProcessor;

public class RandomSGFReaderThread extends Thread {
	private Random random = new Random();
	private String root;
	private String[] files;
	private FeatureGenerator generator;
	private int startMove;
	private int endMove;
	
	public RandomSGFReaderThread(FeatureGenerator generator, String root, String[] files, int startMove, int endMove) {
		this.root = root;
		this.files = files;
		this.generator = generator;
		this.startMove = startMove;
		this.endMove = endMove;
	}
	
	public void run() {
		int size = files.length;
		int count = 0;
		while (true) {
			if (generator.isStop()) break;
			count++;
			if (count % 10000 == 0) System.out.println(count);
			int index = random.nextInt(size);
			String sgfFile = root + files[index];
			if (sgfFile.endsWith(".sgf")) {
				try {
//					System.out.println(sgfFile);
					SgfFeatureProcessor sgfProcessor = new SgfFeatureProcessor(sgfFile);
					List<String> features = sgfProcessor.process(random, 0.01f, startMove, endMove);
					if (features.size() > 0) {
						generator.write2File(features);
					}
				}
				catch (Exception e) {
//					System.err.println(sgfFile);
//					e.printStackTrace();
				}
			}
		}
	}
}
