package net.xdevelop.go.preprocess;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;

public class SpecialFeatureReaderThread extends Thread {
		private Random random = new Random();
		private List<String> features;
		private FeatureGenerator generator;
		private int numPerSecond;
		
		public SpecialFeatureReaderThread(FeatureGenerator generator, String file, int numPerSec) {
			try {
				System.out.println("Loading Special feature...");
				File f = new File(file);
				InputStream in = new BufferedInputStream(new FileInputStream(f));
				features = IOUtils.readLines(in);
				this.generator = generator;
				this.numPerSecond = numPerSec;
				System.out.println("Special feature loaded");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public void run() {
			int size = features.size();
			while (true) {
				if (generator.isStop()) break;
				
				int index = random.nextInt(size);
				String feature = features.get(index);
				generator.write2File(feature);
				try {
					Thread.sleep(1000/numPerSecond);
				} catch (InterruptedException e) {
				}
			}
		}
	}
