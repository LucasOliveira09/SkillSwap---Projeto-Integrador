import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { appConfig } from '../../../../app.config';
import { AnunciosDemoService } from '../../data-access/anuncios-demo.service';

describe('Catálogo com o layout antigo', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: appConfig.providers }));

  it('exibe cinco exemplos identificados sem gravá-los como anúncios da sessão', async () => {
    const harness = await RouterTestingHarness.create('/anuncios');
    expect(harness.routeNativeElement?.querySelectorAll('app-anuncio-card').length).toBe(5);
    expect(harness.routeNativeElement?.textContent).toContain('Exemplos ilustrativos');
    expect(TestBed.inject(AnunciosDemoService).anuncios()).toEqual([]);
  });

  it('busca sem diferenciar acentos e mantém estado vazio quando não há resultado', async () => {
    const harness = await RouterTestingHarness.create('/anuncios?busca=JIU-JITSU');
    expect(harness.routeNativeElement?.querySelectorAll('app-anuncio-card').length).toBe(1);
    await harness.navigateByUrl('/anuncios?busca=servico-inexistente');
    expect(harness.routeNativeElement?.textContent).toContain('Nenhum resultado encontrado');
    await harness.navigateByUrl('/anuncios?busca=futebol');
    expect(harness.routeNativeElement?.textContent).toContain('Aula de Futebol');
  });

  it('inclui os anúncios criados na sessão nos filtros de categoria e busca', async () => {
    TestBed.inject(AnunciosDemoService).criar({
      titulo: 'Aulas de violão',
      descricao: 'Música para iniciantes',
      cidade: 'Florianópolis',
      categoria: 'aulas',
      modalidade: 'troca',
      preco: null,
      habilidadeDesejada: 'Pintura',
    });
    const harness = await RouterTestingHarness.create('/anuncios?busca=violao&categoria=aulas');
    expect(harness.routeNativeElement?.querySelectorAll('app-anuncio-card').length).toBe(1);
    expect(harness.routeNativeElement?.textContent).toContain('Aulas de violão');
    await harness.navigateByUrl('/anuncios?busca=violao&categoria=manutencao');
    expect(harness.routeNativeElement?.textContent).toContain('Nenhum resultado encontrado');
  });
});
