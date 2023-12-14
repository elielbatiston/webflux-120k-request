Os schedulers é uma abstração de pool de threads. 
É o cara que trabalha nas coisas, é um worker

OBS: Quando não damos um subscribeOn, não subescrevemos para uma das threads abaixo 
dessa forma, estaremos nas threads do event loop do netty que não é interessante bloquear essas threads.

=============================================
Schedulers.parallel();
=============================================
Feito especificamente para tarefas que mexem com CPU. 
Abre threads de acordo com a quantidade de CPU que vc tem na maquina

Se eu bloquear uma thread para CPU erroneamente, 
eu vou ter uma Thread a menos para processar todas as coisas que são pra CPU

OBS: O List.of().Stream().parallel() não foi feito pra fazer chamada I/O bloqueante, não é feito para fazer chamada request 
porque ele usa um pool que é o ForkJoinPool.commonPool() que é identico ao Schedulers.parallel() que é feito para tarefas
de CPU como:

* Sorting
* Hashing
* Marshal
* Unmarshal

Se fizermos isso no stream do java e blocarmos as threads desse cara, prejudicamos a JVM inteira

=============================================
Schedulers.boundedElastic();
=============================================
É o equivalente a um scheduler específico para I/O. 
Ou seja, toda chamada que é bloqueante deve ser colocado nessa scheduler.
Por exemplo: 
   * Ir para disco
   * Ir para um banco de dados RBDMS (banco de dados bloqueante, tipo MySQL quando não tinha a implementação reativa)

Esse é um pool de threads que cresce ao infinito (por isso que é Elastic) e não tem controle e reduz conforme não for sendo utilizado. 
Por isso ele foi feito para I/O bloqueante. 

=============================================
Schedulers.immediate();
=============================================

=============================================
Schedulers.single();
=============================================

=============================================
Exemplo de tratamento de exceções. O jeito tradiconal não funciona
=============================================
private final Transaction t = transactionManager.createTransaction() ;

@PostMapping
public Mono<Payment> createPayment() {
    return this.repository.createPayment()
        .flatMap(payment -> this.repository.createPayment(t))
        .flatMap(payment -> t.commit())	
}

=============================================
Exemplo de tratamento de erro
=============================================
@PostMapping
public Mono<Payment> createPayment() {
    return this.repository.createPayment()
        .flatMap(payment -> this.repository.createPayment(t))
        .flatMap(payment -> t.commit())

			//Tratamento de erro classico para fallbacks
			.onErrorContinue(RuntimeException.class, throwable -> {
				return Payment.builder().build();
			})

			// Tratamento de erro direcionando para outra exception (embora seja a mesma).
			// Tipo de uma exceção de negocio para uma exceção HTTP
			.onErrorMap(RuntimeException.class, throwable -> {
				return new RuntimeException(throwable);
			})

			// Parecido com o onErroContinue porem retorno um mono
			// Como se fosse um flatMap
			// Consigo colocar uma outra cadeia (outra pipeline reativa) para resolver esse erro
			.onErrorResume(RuntimeException.class, throwable -> {
				return Mono.error(throwable);
			})
	}
}

=============================================
Exemplo de tratamento de fluxo vazio (vide explicacoes abaixo)
=============================================
@PostMapping
public Mono<Payment> createPayment() {
    return this.repository.createPayment()
    .flatMap(payment -> this.repository.createPayment(t))
    .filter(payment -> Payment.PaymentStatus.APPROVED == payment.getStatus())
    // Se o filter der true ele vai cair no meu flatMap e consequentemente não vai cair no switchIfEmpty
    // Caso contrário vai cair no switchIfEmpty.
    // Assim eu crio uma sequencia lógica com base na minha pipeline
    .flatMap(payment -> {

			})
			// Não chega a ser um tratamento de erro mas sim um tratamento de algo que pode vir a acontecer.
			// Imagine que na cadeia inteira eu não tive um resultado algum. Nenhum cara que eu observo mandou um Mono.empty()
			// Não emitiu um evento propriamente dito, ele só emitiu falando que completou a pipe sem valor algum
			// O defer é pra criar algo pra um futuro, não será criado necessariamente agora
			.switchIfEmpty(Mono.defer(() -> {
				return this.repository.createPayment()
			}));
	});
}


