import { destinoPosLogin } from './conta.component';

describe('destino após login', () => {
  it('retorna ao anúncio solicitado', () => {
    expect(destinoPosLogin('/anuncios/42')).toBe('/anuncios/42');
  });

  it('retorna ao anúncio identificado por código legado', () => {
    expect(destinoPosLogin('/anuncios/exemplo-eletricista')).toBe(
      '/anuncios/exemplo-eletricista',
    );
  });

  it('mantém o retorno ao cadastro de anúncio', () => {
    expect(destinoPosLogin('/anuncios/novo')).toBe('/anuncios/novo');
  });

  it('retorna ao editor de perfil profissional', () => {
    expect(destinoPosLogin('/meu-perfil')).toBe('/meu-perfil');
  });

  it('retorna à conversa interna de um acordo', () => {
    expect(destinoPosLogin('/acordos/15')).toBe('/acordos/15');
  });

  it('usa o catálogo quando o destino não é uma rota interna permitida', () => {
    for (const destino of [null, '', '//externo.example', '/conta', '/anuncios/42?x=1']) {
      expect(destinoPosLogin(destino)).toBe('/anuncios');
    }
  });
});
