/*******************************************************************************
 * mut-learn
 * Copyright (C) 2016 TU Graz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package at.tugraz.mutation_equiv.eval.experiments.tcp.windows8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import at.tugraz.learning.suls.MealyDotFileSul;
import at.tugraz.mutation_equiv.MutationTestCase;
import at.tugraz.mutation_equiv.SimpleTestCase;
import at.tugraz.mutation_equiv.TestCase;
import at.tugraz.mutation_equiv.configuration.RandomCovSelEquivConfiguration;
import at.tugraz.mutation_equiv.eval.Evaluator;
import at.tugraz.mutation_equiv.eval.experiments.MultipleModeConfig;
import at.tugraz.mutation_equiv.eval.experiments.RandomSeedSample;
import at.tugraz.mutation_equiv.mutation.ChangeOutputOperator;
import at.tugraz.mutation_equiv.mutation.MutationOperator;
import at.tugraz.mutation_equiv.mutation.SplitStateOperator;
import at.tugraz.mutation_equiv.mutation.sampling.impl.CompositionSampler;
import at.tugraz.mutation_equiv.mutation.sampling.impl.ElementBasedBoundSampler;
import at.tugraz.mutation_equiv.mutation.sampling.impl.ElementBasedFractionSampler;
import at.tugraz.mutation_equiv.mutation.sampling.impl.IdentitySampler;
import at.tugraz.mutation_equiv.test_selection.MutationSuiteBasedSelector;
import at.tugraz.mutation_equiv.test_selection.RandomSelector;
import at.tugraz.mutation_equiv.trace_gen.AdaptedLeeYannakakisGenerator;
import at.tugraz.mutation_equiv.trace_gen.MuDirectedIterativeGenerator;

public class EvaluateServer {

	@SuppressWarnings("unchecked")
	static <T extends TestCase> List<RandomCovSelEquivConfiguration<T>> configs(MultipleModeConfig config){
		List<RandomCovSelEquivConfiguration<T>> configs = new ArrayList<>();
		List<MutationOperator> ops = new ArrayList<>();
		SplitStateOperator splitOp = new SplitStateOperator(config.sul.getAlphabet(),1);
		// probably too high to be a restriction, but this is intended as we do not want to restrict
		splitOp.setAccSeqBound(200); 
		splitOp.setAllowDiffInLastAccSymbol(true);
		splitOp.setAllowEqualPreStateAndSymbol(true);

		ops.add(splitOp);
		ops.add(new ChangeOutputOperator(config.sul.getAlphabet()));

		RandomCovSelEquivConfiguration<MutationTestCase> mutSplit1OutGenMuDirectedConfig = 
				new RandomCovSelEquivConfiguration<MutationTestCase>(
				ops, config.mutChangeOutputConfig.sizeTestSelectionSuite, false,
				new MutationSuiteBasedSelector(0, config.sul.getAlphabet(), false),
				config.defaultTraceGen,new IdentitySampler());
		mutSplit1OutGenMuDirectedConfig.mutantSampler = new CompositionSampler(
				new ElementBasedFractionSampler(2, new Random(),"operator:split-state-1"),
				new ElementBasedBoundSampler(50000, new Random(),"operator:split-state-1")
				// to reduce mutants for first steps
				);
		
		mutSplit1OutGenMuDirectedConfig.mutantGenerationSampler = 
				new CompositionSampler(
						new ElementBasedBoundSampler(0, new Random(),"operator:split-state-1"),
						new ElementBasedBoundSampler(0, new Random(),"operator:split-state-2"),
						new ElementBasedBoundSampler(0, new Random(),"operator:split-state-3")
						);
		mutSplit1OutGenMuDirectedConfig.keepExecutedTests = true;
		configs.add((RandomCovSelEquivConfiguration<T>) mutSplit1OutGenMuDirectedConfig);
		
		RandomCovSelEquivConfiguration<MutationTestCase> mutChangeOut = config.mutChangeOutputConfig;
		configs.add((RandomCovSelEquivConfiguration<T>) mutChangeOut);
		mutChangeOut.reuseRemaining = false;
		
		RandomCovSelEquivConfiguration<SimpleTestCase> ly = new RandomCovSelEquivConfiguration<SimpleTestCase>(
				Collections.emptyList(), 
				upperNrTests, 
				false, 
				new RandomSelector(0),
				new AdaptedLeeYannakakisGenerator("src/main/resources/lee_yannakakis", 
						"minimal", 4, config.sul.getAlphabet()),
				new IdentitySampler());
		configs.add((RandomCovSelEquivConfiguration<T>) ly);
		ly.nameSuffix = "_ly";
		
		return configs;
	}
	private static int upperNrTests = 96000;
	public static void main(String[] args){
		MealyDotFileSul sul = new MealyDotFileSul("src/main/resources/tcp/tcp_server_windows_trans.dot");
		MultipleModeConfig config = new MultipleModeConfig(40, 300000, sul,
				new MuDirectedIterativeGenerator(0.1, 0.9, sul.getAlphabet(), 40, 6));

		Evaluator evaluator = 
				new Evaluator(RandomSeedSample.seeds,sul, "rv_experiments", "tcp.windows8.server");
		evaluator.setUseRV();
		evaluator.performMultMeasurementSeries(2000, 96000, 2000, sul.getMealy().size(), configs(config));
	}

}
