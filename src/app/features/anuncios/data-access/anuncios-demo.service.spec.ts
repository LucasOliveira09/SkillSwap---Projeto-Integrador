import { TestBed } from '@angular/core/testing';
import { AnunciosDemoService } from './anuncios-demo.service';
import { DadosAnuncio } from '../models/anuncio.model';

describe('AnunciosDemoService', () => {
  let service: AnunciosDemoService;
  const venda: DadosAnuncio = {
    titulo: '  Instalação elétrica  ',
    descricao: '  Instalação de tomadas residenciais.  ',
    cidade: '  Florianópolis  ',
    categoria: 'manutencao',
    modalidade: 'venda',
    preco: 150,
    habilidadeDesejada: '',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AnunciosDemoService);
  });

  it('começa sem anúncios e normaliza os textos ao criar uma venda', () => {
    expect(service.anuncios()).toEqual([]);
    const anuncio = service.criar(venda);
    expect(anuncio.titulo).toBe('Instalação elétrica');
    expect(anuncio.descricao).toBe('Instalação de tomadas residenciais.');
    expect(anuncio.cidade).toBe('Florianópolis');
    expect(service.anuncios()).toEqual([anuncio]);
  });

  for (const campo of ['titulo', 'descricao', 'cidade'] as const) {
    it(`rejeita ${campo} preenchido somente com espaços sem alterar o catálogo`, () => {
      expect(() => service.criar({ ...venda, [campo]: '   ' })).toThrowError();
      expect(service.anuncios()).toEqual([]);
    });
  }

  for (const preco of [null, 0, -10, NaN, Infinity]) {
    it(`rejeita preço de venda inválido: ${preco}`, () => {
      expect(() => service.criar({ ...venda, preco })).toThrowError();
    });
  }

  it('exige a habilidade desejada para uma troca', () => {
    expect(() =>
      service.criar({ ...venda, modalidade: 'troca', habilidadeDesejada: '  ' }),
    ).toThrowError();
  });

  it('cria troca sem preço e não preserva dados de negociação incompatíveis', () => {
    const troca = service.criar({
      ...venda,
      modalidade: 'troca',
      habilidadeDesejada: ' Aulas de música ',
    });
    expect(troca.habilidadeDesejada).toBe('Aulas de música');
    expect(troca.preco).toBeNull();
    const anuncio = service.criar({ ...venda, habilidadeDesejada: 'Valor de outra modalidade' });
    expect(anuncio.habilidadeDesejada).toBe('');
    expect(troca.id).not.toBe(anuncio.id);
  });

  it('rejeita textos maiores que os limites definidos para a demonstração', () => {
    expect(() => service.criar({ ...venda, titulo: 'a'.repeat(101) })).toThrowError();
    expect(() => service.criar({ ...venda, descricao: 'a'.repeat(1501) })).toThrowError();
    expect(() => service.criar({ ...venda, cidade: 'a'.repeat(81) })).toThrowError();
  });

  it('não compartilha anúncios com uma nova instância da aplicação', () => {
    service.criar(venda);
    TestBed.resetTestingModule();
    expect(TestBed.inject(AnunciosDemoService).anuncios()).toEqual([]);
  });
});
