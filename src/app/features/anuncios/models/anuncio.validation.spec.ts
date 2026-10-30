import { DadosAnuncio } from './anuncio.model';
import { validarAnuncio } from './anuncio.validation';

describe('Validacao do formulario', () => {
  const categorias = [{ valor: 'aulas' }];
  const venda: DadosAnuncio = {
    titulo: 'Aula',
    descricao: 'Musica para iniciantes',
    cidade: 'Ourinhos',
    categoria: 'aulas',
    modalidade: 'venda',
    preco: 150,
    habilidadeDesejada: '',
  };
  const validar = (dados: DadosAnuncio) => validarAnuncio(dados, categorias);

  it('aceita venda valida e troca com habilidade desejada', () => {
    expect(validar(venda)).toBeNull();
    expect(
      validar({ ...venda, modalidade: 'troca', preco: null, habilidadeDesejada: 'Pintura' }),
    ).toBeNull();
  });
  for (const campo of ['titulo', 'descricao', 'cidade'] as const) {
    it(`rejeita ${campo} com espacos`, () => {
      expect(validar({ ...venda, [campo]: '   ' })).not.toBeNull();
    });
  }
  for (const preco of [null, 0, -10, NaN, Infinity, 0.001, 100000000]) {
    it(`rejeita preco invalido: ${preco}`, () => {
      expect(validar({ ...venda, preco })).not.toBeNull();
    });
  }
  it('exige habilidade na troca e categoria retornada pela API', () => {
    expect(validar({ ...venda, modalidade: 'troca', habilidadeDesejada: '  ' })).not.toBeNull();
    expect(validar({ ...venda, categoria: 'inexistente' })).not.toBeNull();
    expect(validarAnuncio(venda, [])).not.toBeNull();
  });
  it('preserva limites dos textos', () => {
    expect(validar({ ...venda, titulo: 'a'.repeat(101) })).not.toBeNull();
    expect(validar({ ...venda, descricao: 'a'.repeat(1501) })).not.toBeNull();
    expect(validar({ ...venda, cidade: 'a'.repeat(81) })).not.toBeNull();
    expect(
      validar({ ...venda, modalidade: 'troca', habilidadeDesejada: 'a'.repeat(121) }),
    ).not.toBeNull();
  });
});
