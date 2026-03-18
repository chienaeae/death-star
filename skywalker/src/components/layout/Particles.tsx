import { useEffect, useRef } from 'react';

export function Particles() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d', { alpha: true });
    if (!ctx) return;

    let particlesArray: Particle[] = [];
    let animationFrameId: number;

    const mouse = {
      x: undefined as number | undefined,
      y: undefined as number | undefined,
      radius: 120
    };

    const handleMouseMove = (event: MouseEvent) => {
      mouse.x = event.clientX;
      mouse.y = event.clientY;
    };

    const handleMouseOut = () => {
      mouse.x = undefined;
      mouse.y = undefined;
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseout', handleMouseOut);

    const resize = () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    window.addEventListener('resize', resize);
    resize();

    class Particle {
      x: number;
      y: number;
      size: number;
      speedX: number;
      speedY: number;
      color: string;
      shape: 'circle' | 'square' | 'triangle';
      flickerOffset: number;
      flickerSpeed: number;

      constructor() {
        // Spawn randomly across the entire screen
        this.x = Math.random() * canvas!.width;
        this.y = Math.random() * canvas!.height;
        
        // High-tech silicon valley blues/cyans matching the Death Star glow
        const isCyan = Math.random() > 0.4;
        this.color = isCyan ? 'rgba(0, 220, 255, ' : 'rgba(0, 110, 255, ';
        
        // Abstract sizes simulating distant and close fragments
        this.size = Math.random() * 3 + 0.5;
        this.flickerOffset = Math.random() * Math.PI * 2;
        this.flickerSpeed = Math.random() * 0.05 + 0.01;
        
        // Ambient drifting in random directions
        this.speedX = (Math.random() - 0.5) * 1.5;
        this.speedY = (Math.random() - 0.5) * 1.5;

        // Give fragments technological geometric shapes rather than just circles
        const randShape = Math.random();
        this.shape = randShape > 0.6 ? 'square' : randShape > 0.3 ? 'triangle' : 'circle';
      }

      update() {
        this.x += this.speedX;
        this.y += this.speedY;

        // Add very slight wave/wobble for floating organic effect
        this.y += Math.sin(Date.now() * 0.001 + this.flickerOffset) * 0.3;
        this.x += Math.cos(Date.now() * 0.001 + this.flickerOffset) * 0.3;

        // Anti-gravity effect: Push particles away from the mouse
        if (mouse.x !== undefined && mouse.y !== undefined) {
          const dx = mouse.x - this.x;
          const dy = mouse.y - this.y;
          const distance = Math.sqrt(dx * dx + dy * dy);

          if (distance < mouse.radius) {
            const force = (mouse.radius - distance) / mouse.radius;
            const forceDirectionX = dx / distance;
            const forceDirectionY = dy / distance;
            
            const pushCurrentX = forceDirectionX * force * 2.5;
            const pushCurrentY = forceDirectionY * force * 2.5;

            // Push the particle away
            this.x -= pushCurrentX;
            this.y -= pushCurrentY;
          }
        }

        // Wrap around screen beautifully when drifting out of bounds
        if (this.x < -100) this.x = canvas!.width + 100;
        if (this.x > canvas!.width + 100) this.x = -100;
        if (this.y < -100) this.y = canvas!.height + 100;
        if (this.y > canvas!.height + 100) this.y = -100;
      }

      draw() {
        if (!ctx) return;
        
        // Calculate flickering opacity
        const opacity = (Math.sin(Date.now() * this.flickerSpeed + this.flickerOffset) * 0.3) + 0.6;
        
        ctx.fillStyle = this.color + opacity + ')';
        ctx.shadowBlur = this.size * 3;
        ctx.shadowColor = this.color + '0.8)';
        
        ctx.beginPath();
        if (this.shape === 'circle') {
          ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
        } else if (this.shape === 'square') {
          ctx.rect(this.x, this.y, this.size * 1.5, this.size * 1.5);
        } else if (this.shape === 'triangle') {
          ctx.moveTo(this.x, this.y - this.size);
          ctx.lineTo(this.x + this.size, this.y + this.size);
          ctx.lineTo(this.x - this.size, this.y + this.size);
          ctx.closePath();
        }
        ctx.fill();
        ctx.shadowBlur = 0; // Reset
      }
    }

    const init = () => {
      particlesArray = [];
      // Adjust density
      const numberOfParticles = Math.min(250, (canvas.width * canvas.height) / 8000);
      for (let i = 0; i < numberOfParticles; i++) {
        particlesArray.push(new Particle());
      }
    };

    const animate = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      for (let i = 0; i < particlesArray.length; i++) {
        particlesArray[i].update();
        particlesArray[i].draw();
      }
      animationFrameId = requestAnimationFrame(animate);
    };

    init();
    animate();

    return () => {
      window.removeEventListener('resize', resize);
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseout', handleMouseOut);
      cancelAnimationFrame(animationFrameId);
    };
  }, []);

  return <canvas ref={canvasRef} className="fixed inset-0 pointer-events-none z-[-1]" />;
}
