package com.example.batchprocessing;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

// tag::setup[]
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

	private static final Logger log = LoggerFactory.getLogger(BatchConfiguration.class);

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;
	// end::setup[]

	private static final String QUERY_FINDALL = "select first_name, last_name from persons";

	private static final String OUTPUT_FILE_NAME = "personData.txt";

	private static final String OUTPUT_FILE_PATH = "";

	private static final String OUTPUT_FILE_HEADER = "person data";

	// tag::readerwriterprocessor[]
//	@Bean
//	public FlatFileItemReader<Person> reader() {
//		return new FlatFileItemReaderBuilder<Person>()
//			.name("personItemReader")
//			.resource(new ClassPathResource("sample-data.csv"))
//			.delimited()
//			.names(new String[]{"firstName", "lastName"})
//			.fieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {{
//				setTargetType(Person.class);
//			}})
//			.build();
//	}

	@Bean
	public ItemReader<Person> reader(DataSource dataSource) {
		return new JdbcCursorItemReaderBuilder<Person>()
				.name("personItemReader")
				.dataSource(dataSource)
				.sql(QUERY_FINDALL)
				.rowMapper(new PersonRowMapper())
				.build();
	}

	@Bean
	public ItemWriter<Person> writer(){
//		String exportFilePath = environment.getRequiredProperty(PROPERTY_CSV_EXPORT_FILE_PATH);
		Resource exportFileResource = new FileSystemResource(OUTPUT_FILE_NAME);

//		String exportFileHeader = environment.getRequiredProperty(OUTPUT_FILE_HEADER);
		StringHeaderWriter headerWriter = new StringHeaderWriter(OUTPUT_FILE_HEADER);

		LineAggregator<Person> lineAggregator = createPersonLineAggregator();

		return new FlatFileItemWriterBuilder<Person>()
				.name("personItemWriter")
				.headerCallback(headerWriter)
				.lineAggregator(lineAggregator)
				.resource(exportFileResource)
				.build();
	}

	private LineAggregator<Person> createPersonLineAggregator() {
		DelimitedLineAggregator<Person> lineAggregator =
				new DelimitedLineAggregator<>();
		lineAggregator.setDelimiter("^");

		FieldExtractor<Person> fieldExtractor = createPersonFieldExtractor();
		lineAggregator.setFieldExtractor(fieldExtractor);

		return lineAggregator;
	}

	private FieldExtractor<Person> createPersonFieldExtractor() {
		BeanWrapperFieldExtractor<Person> extractor =
				new BeanWrapperFieldExtractor<>();
		extractor.setNames(new String[] {
				"firstName",
				"lastName"
		});
		return extractor;
	}

	/**
	 * Creates a bean that represents the only step of our batch job.
	 * @param reader
	 * @param writer
	 * @param stepBuilderFactory
	 * @return
	 */
	@Bean
	public Step exampleJobStep(ItemReader<Person> reader,
							   ItemWriter<Person> writer,
							   PersonItemProcessor processor,
							   StepBuilderFactory stepBuilderFactory) {
		return stepBuilderFactory.get("exampleJobStep")
				.<Person, Person>chunk(10)
				.reader(reader)
//				.processor(processor)
				.writer(writer)
				.build();
	}

	/**
	 * Creates a bean that represents our example batch job.
	 * @param exampleJobStep
	 * @param jobBuilderFactory
	 * @return
	 */
	@Bean
	public Job exampleJob(Step exampleJobStep,
						  JobBuilderFactory jobBuilderFactory) {
		return jobBuilderFactory.get("exampleJob")
				.incrementer(new RunIdIncrementer())
				.flow(exampleJobStep)
				.end()
				.build();
	}


	@Bean
	public PersonItemProcessor processor() {
		return new PersonItemProcessor();
	}

	@Bean
	@Qualifier("jdbcTemplateTemp")
	public JdbcTemplate jdbcTemplateTemp(DataSource dataSource){
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<Person> persons = jdbcTemplate.query(QUERY_FINDALL, new PersonRowMapper());
		log.info("Printing data");
		for(Person person: persons){
			log.info(String.valueOf(person));
		}
		return jdbcTemplate;
	}
//	@Bean
//	public JdbcBatchItemWriter<Person> writer(DataSource dataSource) {
//		return new JdbcBatchItemWriterBuilder<Person>()
//			.itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
//			.sql("INSERT INTO people (first_name, last_name) VALUES (:firstName, :lastName)")
//			.dataSource(dataSource)
//			.build();
//	}
//	// end::readerwriterprocessor[]
//
//	// tag::jobstep[]
//	@Bean
//	public Job importUserJob(JobCompletionNotificationListener listener, Step step1) {
//		return jobBuilderFactory.get("importUserJob")
//			.incrementer(new RunIdIncrementer())
//			.listener(listener)
//			.flow(step1)
//			.end()
//			.build();
//	}
//
//	@Bean
//	public Step step1(JdbcBatchItemWriter<Person> writer) {
//		return stepBuilderFactory.get("step1")
//			.<Person, Person> chunk(10)
//			.reader(reader())
//			.processor(processor())
//			.writer(writer)
//			.build();
//	}
	// end::jobstep[]
}
